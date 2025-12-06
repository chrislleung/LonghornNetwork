/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Custom UT Longhorn colors
        'ut-orange': '#bf5700',
        'ut-dark': '#333f48',
      }
    },
  },
  plugins: [],
}