// src/member/work/banking/wallet.ts
import { auth, db } from "@/src/api/firebase";
import {
  doc,
  increment,
  serverTimestamp,
  updateDoc,
  runTransaction,
  addDoc,
  collection,
} from "firebase/firestore";

const SC_PER_GBP = 50;
const MIN_DEBT = -30; // maximum negative balance allowed

// Extend as needed
export type SupportedCurrency =
  | "GBP"
  | "USD"
  | "EUR"
  | "INR"
  | "CNY"
  | "ZAR"
  | "NGN"
  | "RUB"
  | "JPY"
  | "KRW"
  | "BRL"
  | "CAD"
  | "AUD";

export type WalletTxType = "topup" | "credit_out" | "credit_in";

async function logWalletTx(data: {
  userId: string;
  type: WalletTxType;
  amount: number;          // + for in, - for out from this user's view
  balanceAfter?: number;   // optional, if you have it
  note?: string;
  peerUserId?: string;
  tradeId?: string;
  fromUserName?: string;
  toUserName?: string;
}) {
  const base: any = {
    userId: data.userId,
    type: data.type,
    amount: data.amount,
    createdAt: serverTimestamp(),
  };

  if (data.balanceAfter !== undefined) {
    base.balanceAfter = data.balanceAfter;
  }
  if (data.note !== undefined) {
    base.note = data.note;
  }
  if (data.peerUserId !== undefined) {
    base.peerUserId = data.peerUserId;
  }
  if (data.tradeId !== undefined) {
    base.tradeId = data.tradeId;
  }
  if (data.fromUserName !== undefined) {
    base.fromUserName = data.fromUserName;
  }
  if (data.toUserName !== undefined) {
    base.toUserName = data.toUserName;
  }

  await addDoc(collection(db, "walletTransactions"), base);
}

// You will eventually move FX to a backend with a real API and update these placeholders.
async function convertToGBP(
  amount: number,
  currency: SupportedCurrency
): Promise<number> {
  if (currency === "GBP") return amount;

  switch (currency) {
    case "USD": {
      const rateUsdToGbp = 0.8;
      return amount * rateUsdToGbp;
    }
    case "EUR": {
      const rateEurToGbp = 0.85;
      return amount * rateEurToGbp;
    }
    case "INR": {
      const inrPerGbp = 123;
      return amount / inrPerGbp;
    }
    case "CNY": {
      const cnyPerGbp = 9;
      return amount / cnyPerGbp;
    }
    case "ZAR": {
      const zarPerGbp = 24;
      return amount / zarPerGbp;
    }
    case "NGN": {
      const ngnPerGbp = 1900;
      return amount / ngnPerGbp;
    }
    case "RUB": {
      const rubPerGbp = 115;
      return amount / rubPerGbp;
    }
    case "JPY": {
      const jpyPerGbp = 190;
      return amount / jpyPerGbp;
    }
    case "KRW": {
      const krwPerGbp = 1700;
      return amount / krwPerGbp;
    }
    case "BRL": {
      const brlPerGbp = 6.5;
      return amount / brlPerGbp;
    }
    case "CAD": {
      const cadToGbp = 0.6;
      return amount * cadToGbp;
    }
    case "AUD": {
      const audToGbp = 0.55;
      return amount * audToGbp;
    }
    default:
      throw new Error("Unsupported currency");
  }
}

// amount = what the user actually paid in their local currency
export async function buyStarCoins(
  amount: number,
  currency: SupportedCurrency
): Promise<number> {
  const user = auth.currentUser;
  if (!user) throw new Error("Not signed in");

  const amountGbp = await convertToGBP(amount, currency);
  const scToAdd = Math.floor(amountGbp * SC_PER_GBP);

  const userRef = doc(db, "users", user.uid);

  await updateDoc(userRef, {
    scBalance: increment(scToAdd),
    walletLastTopupAt: serverTimestamp(),
  });

  await logWalletTx({
    userId: user.uid,
    type: "topup",
    amount: scToAdd,
    note: `Top‑up (${amount} ${currency})`,
  });

  return scToAdd;
}

// Credit flow: send SC even if sender is at 0, but clamp debt to -30.
export async function creditUserFromTrade(opts: {
  fromUserId: string;  // payer (current user)
  toUserId: string;    // receiver (trade creator)
  amount: number;      // SC to send
  tradeId?: string;    // optional, for auditing
}) {
  const { fromUserId, toUserId, amount, tradeId } = opts;

  if (amount <= 0) {
    throw new Error("Amount must be positive");
  }
  if (fromUserId === toUserId) {
    throw new Error("Cannot credit yourself");
  }

  const fromRef = doc(db, "users", fromUserId);
  const toRef = doc(db, "users", toUserId);

  await runTransaction(db, async (tx) => {
    const fromSnap = await tx.get(fromRef);
    const toSnap = await tx.get(toRef);

    if (!fromSnap.exists()) {
      throw new Error("Sender profile missing");
    }
    if (!toSnap.exists()) {
      throw new Error("Receiver profile missing");
    }

    const fromData = fromSnap.data() as any;
    const toData = toSnap.data() as any;

    const fromName = fromData.nickname || fromData.email || "Unknown";
    const toName = toData.nickname || toData.email || "Unknown";

    const currentFromBalance = Number(fromData.scBalance ?? 0);
    const currentToBalance = Number(toData.scBalance ?? 0);

    const rawNewFrom = currentFromBalance - amount;
    const newFromBalance =
      rawNewFrom < MIN_DEBT ? MIN_DEBT : rawNewFrom;

    const newToBalance = currentToBalance + amount;

    tx.update(fromRef, {
      scBalance: newFromBalance,
      lastActiveAt: serverTimestamp(),
    });

    tx.update(toRef, {
      scBalance: newToBalance,
      lastActiveAt: serverTimestamp(),
    });

    // Log both sides outside of the transaction
    setTimeout(() => {
      // Sender: debit
      logWalletTx({
        userId: fromUserId,
        type: "credit_out",
        amount: -amount,
        balanceAfter: newFromBalance,
        peerUserId: toUserId,
        tradeId,
        note: "Credited another user from trade",
        fromUserName: fromName,
        toUserName: toName,
      });

      // Receiver: credit
      logWalletTx({
        userId: toUserId,
        type: "credit_in",
        amount,
        balanceAfter: newToBalance,
        peerUserId: fromUserId,
        tradeId,
        note: "Received credit from trade",
        fromUserName: fromName,
        toUserName: toName,
      });
    }, 0);
  });
}
