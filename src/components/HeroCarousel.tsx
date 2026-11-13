import React, { useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, ArrowRight, MessageCircle, Truck, Store } from 'lucide-react';
import { HeroBanner } from '../types';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface HeroCarouselProps {
  banners: HeroBanner[];
  lang?: StoreLang;
  onAction: (banner: HeroBanner) => void;
  onShopAll: () => void;
}

export const HeroCarousel: React.FC<HeroCarouselProps> = ({
  banners,
  lang = 'en',
  onAction,
  onShopAll,
}) => {
  const t = getStoreTranslations(lang);
  const activeSlides = banners.filter((b) => b.active !== false);
  const [index, setIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);

  useEffect(() => {
    if (activeSlides.length <= 1) return;
    const timer = setInterval(() => {
      setIndex((prev) => (prev + 1) % activeSlides.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [activeSlides.length]);

  useEffect(() => {
    if (index >= activeSlides.length && activeSlides.length > 0) {
      setIndex(0);
    }
  }, [activeSlides.length, index]);

  if (activeSlides.length === 0) return null;
  const current = activeSlides[index] || activeSlides[0];
  const heroImage =
    index === 0
      ? '/images/store/hero_shopping_family.jpg'
      : current.imageUrl || '/images/store/hero_shopping_family.jpg';

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return;
    const delta = e.changedTouches[0].clientX - touchStartX.current;
    if (Math.abs(delta) > 45) {
      if (delta < 0) {
        setIndex((prev) => (prev + 1) % activeSlides.length);
      } else {
        setIndex((prev) => (prev - 1 + activeSlides.length) % activeSlides.length);
      }
    }
    touchStartX.current = null;
  };

  return (
    <section
      className="relative rounded-2xl overflow-hidden bg-[#0F3D28] text-[#F8F7F2] border border-[#0F3D28]/20 shadow-sm"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      aria-label="Nav Grocery Store Top Shopping Banner"
    >
      <div className="grid grid-cols-1 lg:grid-cols-12 items-stretch min-h-[300px] lg:min-h-[360px]">
        <div className="lg:col-span-6 p-6 sm:p-8 lg:p-10 flex flex-col justify-between gap-5 z-10 bg-[#0F3D28]">
          <div className="space-y-3">
            <p className="text-[11px] sm:text-xs font-semibold tracking-wider uppercase text-[#E6A817]">
              {t.heroEyebrow}
            </p>
            <h1 className="font-display text-2xl sm:text-3xl lg:text-4xl font-bold leading-tight text-balance text-white">
              {index === 0 ? t.heroTitle : current.title}
            </h1>
            <p className="text-sm sm:text-base text-[#F8F7F2]/85 max-w-xl leading-relaxed">
              {index === 0 ? t.heroSubtitle : current.subtitle}
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1 text-xs text-[#F8F7F2]/90">
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-[#E6A817] shrink-0" />
                <span>{t.heroDeliveryBadge}</span>
              </div>
              <div className="flex items-center gap-2">
                <Store className="w-4 h-4 text-[#E6A817] shrink-0" />
                <span>{t.heroPickupBadge}</span>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 pt-1">
            <button
              type="button"
              onClick={index === 0 ? onShopAll : () => onAction(current)}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-[#E6A817] text-[#14221B] font-bold text-sm hover:bg-[#f0b72b] transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              <span>{index === 0 ? t.heroShopAllCta : current.ctaLabel}</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <a
              href="https://wa.me/918847370905?text=Hello%20Nav%20Grocery%20Store%2C%20I%20would%20like%20to%20place%20a%20grocery%20order."
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-white/10 hover:bg-white/20 border border-white/25 text-white font-semibold text-sm transition-colors whitespace-nowrap shrink-0"
            >
              <MessageCircle className="w-4 h-4 text-[#E6A817]" />
              <span>{t.heroWhatsAppCta}</span>
            </a>

            {activeSlides.length > 1 && (
              <div className="flex items-center gap-1.5 ml-auto">
                {activeSlides.map((slide, idx) => (
                  <button
                    key={slide.id}
                    type="button"
                    aria-label={`Show banner ${idx + 1}`}
                    onClick={() => setIndex(idx)}
                    className={`h-2 rounded-full transition-all cursor-pointer ${
                      idx === index ? 'w-7 bg-[#E6A817]' : 'w-2 bg-white/35 hover:bg-white/60'
                    }`}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="lg:col-span-6 relative min-h-[210px] sm:min-h-[260px] lg:min-h-full bg-[#0A2A1B]">
          <img
            src={heroImage}
            alt="Nav Grocery Store Sandhaur family shopping packaged groceries and frozen foods"
            className="w-full h-full object-cover object-center"
            referrerPolicy="no-referrer"
            onError={(e) => {
              const target = e.currentTarget;
              if (!target.dataset.fallbackApplied) {
                target.dataset.fallbackApplied = '1';
                target.src = '/images/store/exterior.jpg';
              }
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-t lg:bg-gradient-to-r from-[#0F3D28]/95 via-[#0F3D28]/20 to-transparent pointer-events-none" />
        </div>
      </div>

      {activeSlides.length > 1 && (
        <>
          <button
            type="button"
            aria-label="Previous banner"
            onClick={() => setIndex((prev) => (prev - 1 + activeSlides.length) % activeSlides.length)}
            className="hidden sm:flex absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/35 hover:bg-black/55 text-white items-center justify-center transition-colors cursor-pointer z-20"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <button
            type="button"
            aria-label="Next banner"
            onClick={() => setIndex((prev) => (prev + 1) % activeSlides.length)}
            className="hidden sm:flex absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/35 hover:bg-black/55 text-white items-center justify-center transition-colors cursor-pointer z-20"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </>
      )}
    </section>
  );
};
