import React from 'react';
import { Loader2 } from 'lucide-react';

interface GoogleSignInButtonProps {
  onClick: () => void;
  loading?: boolean;
  label?: string;
  variant?: 'gold' | 'white' | 'dark' | 'compact';
  className?: string;
}

export const GoogleLogoMark: React.FC<{ className?: string }> = ({ className = 'w-4 h-4' }) => (
  <svg viewBox="0 0 24 24" className={`${className} shrink-0`} aria-hidden="true">
    <path
      fill="#4285F4"
      d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12v4.51h6.47a5.54 5.54 0 0 1-2.4 3.63v3h3.88c2.27-2.09 3.54-5.17 3.54-8.87z"
    />
    <path
      fill="#34A853"
      d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.09A12 12 0 0 0 12 24z"
    />
    <path
      fill="#FBBC05"
      d="M5.28 14.29A7.21 7.21 0 0 1 4.9 12c0-.79.14-1.56.38-2.29V6.62H1.29A12 12 0 0 0 0 12c0 1.94.46 3.77 1.29 5.38l3.99-3.09z"
    />
    <path
      fill="#EA4335"
      d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0A12 12 0 0 0 1.29 6.62l3.99 3.09c.95-2.85 3.6-4.96 6.72-4.96z"
    />
  </svg>
);

export const GoogleSignInButton: React.FC<GoogleSignInButtonProps> = ({
  onClick,
  loading = false,
  label = 'Continue with Google',
  variant = 'white',
  className = '',
}) => {
  const styles: Record<NonNullable<GoogleSignInButtonProps['variant']>, string> = {
    white:
      'bg-white text-[#14221B] border border-[#0F3D28]/20 hover:bg-[#F8F7F2] px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold shadow-xs',
    gold:
      'bg-[#E6A817] text-[#14221B] hover:bg-[#f0b72b] px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold shadow-xs',
    dark:
      'bg-[#0F3D28] text-white hover:bg-[#0A2A1B] px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold shadow-xs',
    compact:
      'bg-white text-[#14221B] hover:bg-[#F8F7F2] px-3 py-1.5 rounded-lg text-[11px] font-bold',
  };

  return (
    <button
      type="button"
      disabled={loading}
      onClick={onClick}
      className={`inline-flex items-center justify-center gap-2 whitespace-nowrap cursor-pointer transition-colors disabled:opacity-60 ${styles[variant]} ${className}`}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : (
        <GoogleLogoMark className={variant === 'compact' ? 'w-3.5 h-3.5' : 'w-4 h-4'} />
      )}
      <span>{loading ? 'Signing in...' : label}</span>
    </button>
  );
};
