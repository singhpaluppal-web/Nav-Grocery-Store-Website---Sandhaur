import React, { useState } from 'react';

interface StoreLogoProps {
  logoUrl?: string;
  alt?: string;
  sizeClass?: string;
  darkSurface?: boolean;
}

export const StoreLogo: React.FC<StoreLogoProps> = ({
  logoUrl = '/images/logo.png',
  alt = 'Nav Grocery Store Official Crest Logo',
  sizeClass = 'w-11 h-11',
  darkSurface = false,
}) => {
  const [failed, setFailed] = useState(false);

  if (!failed && logoUrl) {
    return (
      <div
        className={`${sizeClass} rounded-full overflow-hidden shrink-0 flex items-center justify-center border-2 shadow-xs ${
          darkSurface
            ? 'border-[#E6A817] bg-[#0A2A1B]'
            : 'border-[#0F3D28]/25 ring-2 ring-[#E6A817]/50 bg-[#0F3D28]'
        }`}
      >
        <img
          src={logoUrl}
          alt={alt}
          className="w-full h-full object-cover"
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
        />
      </div>
    );
  }

  return (
    <div
      className={`${sizeClass} rounded-full shrink-0 flex items-center justify-center border-2 border-[#E6A817] bg-[#0F3D28] text-[#E6A817] font-display font-bold select-none`}
      aria-label={alt}
    >
      <svg viewBox="0 0 64 64" className="w-full h-full p-1.5" role="img" aria-label={alt}>
        <circle cx="32" cy="32" r="27" fill="none" stroke="#E6A817" strokeWidth="2.5" />
        <text
          x="32"
          y="39"
          textAnchor="middle"
          fill="#F8F7F2"
          fontFamily="Georgia, serif"
          fontWeight="700"
          fontSize="21"
        >
          NG
        </text>
      </svg>
    </div>
  );
};
