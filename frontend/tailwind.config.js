/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        cream: "#f7f4ed",
        "cream-surface": "#f7f4ed",
        charcoal: "#1c1c1c",
        "off-white": "#fcfbf8",
        "muted-gray": "#5f5f5d",
        "light-cream": "#eceae4",
        "warm-border": "#b8aea0",
        "ring-blue": "rgba(59, 130, 246, 0.5)",
      },
      fontFamily: {
        sans: [
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "Segoe UI",
          "Roboto",
          "sans-serif",
        ],
      },
      boxShadow: {
        focus: "rgba(0, 0, 0, 0.1) 0px 4px 12px",
        "btn-inset":
          "rgba(255,255,255,0.2) 0px 0.5px 0px 0px inset, rgba(0,0,0,0.2) 0px 0px 0px 0.5px inset, rgba(0,0,0,0.05) 0px 1px 2px 0px",
      },
    },
  },
  plugins: [],
};
