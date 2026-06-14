import { create } from "zustand";

interface TradingState {
  darkMode: boolean;
  enableDarkMode: () => void;
  disableDarkMode: () => void;
}

export const useTradingStore = create<TradingState>((set) => ({
  darkMode: false,
  enableDarkMode: () => set({ darkMode: true }),
  disableDarkMode: () => set({ darkMode: false }),
}));
