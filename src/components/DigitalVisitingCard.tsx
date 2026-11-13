import React, { useState } from 'react';
import {
  Phone,
  MessageCircle,
  Download,
  Share2,
  MapPin,
  Mail,
  Globe,
  Instagram,
  Youtube,
  Check,
} from 'lucide-react';
import { StoreLogo } from './StoreLogo';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface DigitalVisitingCardProps {
  logoUrl?: string;
  lang?: StoreLang;
}

export const DigitalVisitingCard: React.FC<DigitalVisitingCardProps> = ({
  logoUrl = '/images/logo.png',
  lang = 'en',
}) => {
  const t = getStoreTranslations(lang);
  const [sharedStatus, setSharedStatus] = useState<'idle' | 'copied'>('idle');

  const handleDownloadVCard = () => {
    const vcardLines = [
      'BEGIN:VCARD',
      'VERSION:3.0',
      'FN:Nav Grocery Store',
      'ORG:Nav Grocery Store - Sandhaur',
      'TEL;TYPE=WORK,VOICE:+918847370905',
      'TEL;TYPE=CELL:+919915752905',
      'EMAIL:orders@navgrocerystore.com',
      'URL:https://www.navgrocerystore.com',
      'ADR;TYPE=WORK:;;Main Bazaar, Sandhaur;Distt. Malerkotla;Punjab;148020;India',
      'NOTE:100% Sealed Branded Groceries & Cold-Chain Frozen Foods (-18C). Flat 12% Off MRP.',
      'END:VCARD',
    ].join('\r\n');

    const blob = new Blob([vcardLines], { type: 'text/vcard;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'Nav_Grocery_Store_Sandhaur.vcf';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleShareCard = async () => {
    const shareText =
      'Nav Grocery Store — Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab 148020. Flat 12% Off MRP on 308 Sealed Groceries & -18°C Frozen Essentials. Helpline: +91 8847370905 · https://www.navgrocerystore.com';

    if (navigator.share) {
      try {
        await navigator.share({
          title: 'Nav Grocery Store — Official Digital Visiting Card',
          text: shareText,
          url: 'https://www.navgrocerystore.com',
        });
        return;
      } catch {
        // user dismissed or native share unavailable; fall back to clipboard copy
      }
    }

    try {
      await navigator.clipboard.writeText(shareText);
      setSharedStatus('copied');
      setTimeout(() => setSharedStatus('idle'), 2600);
    } catch {
      // ignore clipboard restriction
    }
  };

  return (
    <section
      id="digital-visiting-card"
      aria-label="Official Nav Grocery Store Digital Visiting Card"
      className="rounded-2xl overflow-hidden bg-[#0F3D28] text-[#F8F7F2] border border-[#E6A817]/40 shadow-sm"
    >
      <div className="p-6 sm:p-8 lg:p-10 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        {/* Left Identity Column */}
        <div className="lg:col-span-7 space-y-5">
          <div className="flex items-center gap-4">
            <StoreLogo logoUrl={logoUrl} darkSurface sizeClass="w-16 h-16 sm:w-20 sm:h-20" />
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-widest text-[#E6A817]">
                {t.vcardEyebrow}
              </p>
              <h2 className="font-display text-2xl sm:text-3xl font-bold text-white leading-tight">
                {t.storeName}
              </h2>
              <p className="text-xs sm:text-sm text-[#F8F7F2]/85 mt-0.5">
                {t.vcardTagline}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 pt-2 border-t border-white/15 text-xs sm:text-sm">
            <div className="flex items-start gap-2.5">
              <MapPin className="w-4 h-4 text-[#E6A817] shrink-0 mt-0.5" />
              <span>{t.fullAddress}</span>
            </div>
            <div className="flex items-start gap-2.5">
              <Phone className="w-4 h-4 text-[#E6A817] shrink-0 mt-0.5" />
              <div className="tabular-nums">
                <a href="tel:+918847370905" className="hover:text-[#E6A817] underline-offset-2 hover:underline">
                  +91 8847370905
                </a>
                <span className="mx-1.5 text-white/45">·</span>
                <a href="tel:+919915752905" className="hover:text-[#E6A817] underline-offset-2 hover:underline">
                  +91 9915752905
                </a>
              </div>
            </div>
            <div className="flex items-center gap-2.5">
              <Mail className="w-4 h-4 text-[#E6A817] shrink-0" />
              <a
                href="mailto:orders@navgrocerystore.com"
                className="hover:text-[#E6A817] underline-offset-2 hover:underline truncate"
              >
                orders@navgrocerystore.com
              </a>
            </div>
            <div className="flex items-center gap-2.5">
              <Globe className="w-4 h-4 text-[#E6A817] shrink-0" />
              <a
                href="https://www.navgrocerystore.com"
                target="_blank"
                rel="noreferrer"
                className="hover:text-[#E6A817] underline-offset-2 hover:underline"
              >
                www.navgrocerystore.com
              </a>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-4 text-xs text-white/85 pt-1">
            <a
              href="https://www.instagram.com/nav_grocery_store/"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 hover:text-[#E6A817] transition-colors"
            >
              <Instagram className="w-4 h-4 text-[#E6A817]" />
              <span>Instagram: @nav_grocery_store</span>
            </a>
            <span className="text-white/30" aria-hidden="true">·</span>
            <a
              href="https://www.youtube.com/@shopatnav"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 hover:text-[#E6A817] transition-colors"
            >
              <Youtube className="w-4 h-4 text-[#E6A817]" />
              <span>YouTube: @shopatnav</span>
            </a>
          </div>
        </div>

        {/* Right Interactive One-Tap Actions */}
        <div className="lg:col-span-5 bg-[#0A2A1B] rounded-xl p-5 border border-[#E6A817]/25 space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wider text-[#E6A817]">
            {t.vcardActionsTitle}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <a
              href="tel:+918847370905"
              className="px-4 py-2.5 rounded-lg bg-[#E6A817] text-[#14221B] font-bold text-xs sm:text-sm hover:bg-[#f0b72b] transition-colors inline-flex items-center justify-center gap-2 whitespace-nowrap"
            >
              <Phone className="w-4 h-4 shrink-0" />
              <span>{t.vcardCallNow}</span>
            </a>

            <a
              href="https://wa.me/918847370905?text=Sat%20Sri%20Akal%20Nav%20Grocery%20Store!"
              target="_blank"
              rel="noreferrer"
              className="px-4 py-2.5 rounded-lg bg-white text-[#0F3D28] font-bold text-xs sm:text-sm hover:bg-[#F8F7F2] transition-colors inline-flex items-center justify-center gap-2 whitespace-nowrap"
            >
              <MessageCircle className="w-4 h-4 shrink-0" />
              <span>{t.vcardWhatsAppChat}</span>
            </a>

            <button
              type="button"
              onClick={handleDownloadVCard}
              className="px-4 py-2.5 rounded-lg bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold text-xs sm:text-sm transition-colors inline-flex items-center justify-center gap-2 whitespace-nowrap cursor-pointer"
            >
              <Download className="w-4 h-4 text-[#E6A817] shrink-0" />
              <span>{t.vcardSaveContact}</span>
            </button>

            <button
              type="button"
              onClick={handleShareCard}
              className="px-4 py-2.5 rounded-lg bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold text-xs sm:text-sm transition-colors inline-flex items-center justify-center gap-2 whitespace-nowrap cursor-pointer"
            >
              {sharedStatus === 'copied' ? (
                <>
                  <Check className="w-4 h-4 text-[#E6A817] shrink-0" />
                  <span>{t.vcardCopied}</span>
                </>
              ) : (
                <>
                  <Share2 className="w-4 h-4 text-[#E6A817] shrink-0" />
                  <span>{t.vcardShareCard}</span>
                </>
              )}
            </button>
          </div>
          <p className="text-[11px] text-white/70 leading-relaxed">
            {t.vcardHoursNote}
          </p>
        </div>
      </div>
    </section>
  );
};
