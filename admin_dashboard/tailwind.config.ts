import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Duolingo-inspired colors
        'duo-green': '#58CC02',
        'duo-blue': '#2B70C9',
        'duo-orange': '#FF9600',
        'duo-red': '#FF4B4B',
        'duo-purple': '#A855F7',
        'duo-bg': '#FAFAFA',
        'duo-text': '#4B4B4B',
        'duo-border': '#E5E5E5',
      },
      fontFamily: {
        nunito: ['var(--font-nunito)', 'sans-serif'],
      },
      borderRadius: {
        'duo': '16px',
        'duo-lg': '20px',
      },
      boxShadow: {
        'duo': '0 4px 0 rgba(0, 0, 0, 0.1)',
        'duo-sm': '0 2px 0 rgba(0, 0, 0, 0.1)',
      },
    },
  },
  plugins: [],
}
export default config

