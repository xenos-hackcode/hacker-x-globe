// functions/src/billing.ts
import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Stripe from "stripe";
import { db } from "./firebaseAdmin";

const STRIPE_SECRET_KEY = defineSecret("STRIPE_SECRET_KEY");
const STRIPE_WEBHOOK_SECRET = defineSecret("STRIPE_WEBHOOK_SECRET");

export const stripeWebhook = onRequest(
  { secrets: [STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET] },
  async (req, res) => {
    const sig = req.headers["stripe-signature"] as string | undefined;
    if (!sig) {
      res.status(400).send("Missing signature");
      return;
    }

    const stripe = new Stripe(STRIPE_SECRET_KEY.value(), {
      apiVersion: "2026-06-24.dahlia",
    });

    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(
        req.rawBody,
        sig,
        STRIPE_WEBHOOK_SECRET.value()
      );
    } catch (err: any) {
      console.error("Webhook signature verification failed", err?.message);
      res.status(400).send("Webhook Error");
      return;
    }

    try {
      switch (event.type) {
        case "checkout.session.completed": {
          const session = event.data.object as Stripe.Checkout.Session;

          const userId = session.metadata?.userId;
          const kind = (session.metadata?.kind ?? "oneoff") as
            | "oneoff"
            | "subscription";

          if (!userId) break;

          const amountTotal = (session.amount_total ?? 0) / 100;

          const vipGain =
            kind === "subscription" ? amountTotal * 50 : amountTotal * 100;

          const userRef = db.collection("users").doc(userId);
          const txRef = db.collection("walletTransactions").doc();

          await db.runTransaction(async (tx) => {
            const snap = await tx.get(userRef);
            const data = snap.data() || {};
            const currentVipExp = data.vipExp ?? 0;

            tx.set(
              txRef,
              {
                userId,
                amountGBP: amountTotal,
                kind,
                stripeSessionId: session.id,
                status: "paid",
                createdAt: new Date(),
              },
              { merge: true }
            );

            tx.set(
              userRef,
              {
                vipExp: currentVipExp + Math.floor(vipGain),
                walletLastTopupAt: new Date(),
              },
              { merge: true }
            );
          });

          break;
        }

        case "charge.refunded": {
          const charge = event.data.object as Stripe.Charge;
          const userId = charge.metadata?.userId;
          const kind = (charge.metadata?.kind ?? "oneoff") as
            | "oneoff"
            | "subscription";

          if (!userId) break;

          const amount = (charge.amount ?? 0) / 100;
          const vipLoss =
            kind === "subscription" ? amount * 50 : amount * 100;

          const userRef = db.collection("users").doc(userId);

          await db.runTransaction(async (tx) => {
            const snap = await tx.get(userRef);
            const data = snap.data() || {};
            const currentVipExp = data.vipExp ?? 0;
            const next = Math.max(0, currentVipExp - Math.floor(vipLoss));

            tx.set(
              userRef,
              { vipExp: next },
              { merge: true }
            );
          });

          break;
        }

        default:
          break;
      }

      res.status(200).send("ok");
    } catch (err) {
      console.error("stripeWebhook handler error", err);
      res.status(500).send("Server error");
    }
  }
);
