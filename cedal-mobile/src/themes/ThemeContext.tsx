// src/theme/ThemeContext.tsx
import React, { createContext, useContext, useState, ReactNode } from "react";

type Theme = {
  isDark: boolean;
  toggleTheme: () => void;
  colors: {
    background: string;
    headerBackground: string;
    textPrimary: string;
    textSecondary: string;
    border: string;
    chatRowBackground: string;
  };
};

const ThemeContext = createContext<Theme | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [isDark, setIsDark] = useState(true);

  function toggleTheme() {
    setIsDark((prev) => !prev);
  }

  const colors = isDark
    ? {
        background: "#020617",
        headerBackground: "#020617",
        textPrimary: "#e5e7eb",
        textSecondary: "#9ca3af",
        border: "rgba(148,163,184,0.35)",
        chatRowBackground: "#020617",
      }
    : {
        background: "#ffffff",
        headerBackground: "#f9fafb",
        textPrimary: "#020617",
        textSecondary: "#4b5563",
        border: "rgba(156,163,175,0.6)",
        chatRowBackground: "#ffffff",
      };

  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme, colors }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside ThemeProvider");
  return ctx;
}
