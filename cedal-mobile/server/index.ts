// server/index.ts
import express from "express";
import Stripe from "stripe";
import cors from "cors";

const app = express();
app.use(cors());
app.use(express.json());

// Load STRIPE_SECRET_KEY from env (e.g. .env, hosting config, etc.)
const stripeSecretKey = process.env.STRIPE_SECRET_KEY;
if (!stripeSecretKey) {
  throw new Error("STRIPE_SECRET_KEY is not set in environment");
}

// One Stripe client, re‑used for all requests
const stripe = new Stripe(stripeSecretKey, {
  apiVersion: "2026-01-28.clover",
});

// 50 SC for £1 => amount = 100 (pence)
app.post("/create-payment-intent", async (req, res) => {
  try {
    const paymentIntent = await stripe.paymentIntents.create({
      amount: 100, // 1.00 GBP in pence
      currency: "gbp",
      automatic_payment_methods: { enabled: true },
    });

    res.json({
      clientSecret: paymentIntent.client_secret,
    });
  } catch (err: any) {
    console.error("create-payment-intent error", err);
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log("Server listening on", PORT);
});
