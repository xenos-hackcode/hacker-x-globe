// src/member/work/banking/BankSettingsContext.tsx
import React, { createContext, useContext, useState } from "react";

type BankSettingsContextValue = {
  muteAllBankNotifications: boolean;
  setMuteAllBankNotifications: (v: boolean) => void;

  securityAlertsEnabled: boolean;
  setSecurityAlertsEnabled: (v: boolean) => void;

  largeSpendAlertsEnabled: boolean;
  setLargeSpendAlertsEnabled: (v: boolean) => void;
};

const BankSettingsContext = createContext<BankSettingsContextValue | null>(null);

export function BankSettingsProvider({ children }: { children: React.ReactNode }) {
  const [muteAllBankNotifications, setMuteAllBankNotifications] = useState(false);
  const [securityAlertsEnabled, setSecurityAlertsEnabled] = useState(true);
  const [largeSpendAlertsEnabled, setLargeSpendAlertsEnabled] = useState(true);

  return (
    <BankSettingsContext.Provider
      value={{
        muteAllBankNotifications,
        setMuteAllBankNotifications,
        securityAlertsEnabled,
        setSecurityAlertsEnabled,
        largeSpendAlertsEnabled,
        setLargeSpendAlertsEnabled,
      }}
    >
      {children}
    </BankSettingsContext.Provider>
  );
}

export function useBankSettings() {
  const ctx = useContext(BankSettingsContext);
  if (!ctx) {
    throw new Error("useBankSettings must be used inside BankSettingsProvider");
  }
  return ctx;
}
