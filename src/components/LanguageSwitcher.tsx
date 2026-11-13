import React from 'react';
import { Globe } from 'lucide-react';
import { StoreLang } from '../utils/i18n';

interface LanguageSwitcherProps {
  lang: StoreLang;
  onChange: (lang: StoreLang) => void;
  compact?: boolean;
}

const OPTIONS: { code: StoreLang; label: string; ariaLabel: string }[] = [
  { code: 'en', label: 'EN', ariaLabel: 'Switch language to English' },
  { code: 'pa', label: 'ਪੰਜਾਬੀ', ariaLabel: 'Switch language to Punjabi' },
  { code: 'hi', label: 'हिन्दी', ariaLabel: 'Switch language to Hindi' },
];

export const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({
  lang,
  onChange,
  compact = false,
}) => {
  return (
    <div
      role="group"
      aria-label="Store Language Switcher"
      className="inline-flex items-center gap-0.5 bg-[#0A2A1B] rounded-xl p-1 border border-white/10 shrink-0"
    >
      {!compact && <Globe className="w-3.5 h-3.5 text-[#E6A817] ml-1.5 mr-1 hidden sm:block shrink-0" />}
      {OPTIONS.map((opt) => {
        const active = lang === opt.code;
        return (
          <button
            key={opt.code}
            type="button"
            onClick={() => onChange(opt.code)}
            aria-pressed={active}
            aria-label={opt.ariaLabel}
            className={`px-2 sm:px-2.5 py-1 rounded-lg text-[11px] sm:text-xs font-bold whitespace-nowrap shrink-0 transition-colors cursor-pointer ${
              active
                ? 'bg-[#E6A817] text-[#14221B]'
                : 'text-white/85 hover:text-white hover:bg-white/10'
            }`}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
};
