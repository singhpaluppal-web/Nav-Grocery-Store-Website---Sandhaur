import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          emerald: '#0F5132',
          'emerald-dark': '#0A3622',
          'emerald-light': '#157347',
          gold: '#FFC107',
          'gold-hover': '#E0A800',
          surface: '#F8F9FA',
          charcoal: '#212529',
          card: '#FFFFFF',
          border: '#E9ECEF',
          danger: '#DC3545',
        },
      },
      borderRadius: {
        card: '12px',
        pill: '9999px',
      },
      boxShadow: {
        soft: '0 2px 8px rgba(15, 81, 50, 0.08)',
        elevated: '0 6px 18px rgba(15, 81, 50, 0.14)',
        modal: '0 12px 32px rgba(0, 0, 0, 0.22)',
      },
    },
  },
  plugins: [],
};

export default config;
