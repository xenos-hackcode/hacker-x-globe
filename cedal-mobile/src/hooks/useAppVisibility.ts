// src/hooks/useAppVisibility.ts
import { useEffect, useRef } from "react";
import { AppState, AppStateStatus } from "react-native";

export function useAppVisibility(
  onBackground: () => void,
  onForeground: () => void
) {
  const appState = useRef<AppStateStatus>(AppState.currentState);

  useEffect(() => {

    const sub = AppState.addEventListener("change", (nextState) => {
      const prevState = appState.current;

      if (
        prevState === "active" &&
        (nextState === "background" || nextState === "inactive")
      ) {
        onBackground();
      }

      if (
        (prevState === "background" || prevState === "inactive") &&
        nextState === "active"
      ) {
        onForeground();
      }

      appState.current = nextState;
    });

    return () => {
      sub.remove();
    };
  }, [onBackground, onForeground]);
}

