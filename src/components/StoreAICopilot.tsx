import React, { useEffect, useRef, useState } from 'react';
import { Bot, X, Send, Plus, MessageCircle } from 'lucide-react';
import { Product } from '../types';
import { StoreLang } from '../utils/i18n';

interface StoreAICopilotProps {
  products: Product[];
  lang?: StoreLang;
  onChangeLang?: (lang: StoreLang) => void;
  onAddToCart: (product: Product) => void;
  onSelectAisle: (aisleId: string) => void;
}

interface ChatMessage {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  matches?: Product[];
}

const STOP_WORDS = new Set([
  'is', 'are', 'do', 'you', 'have', 'any', 'stock', 'available', 'availability', 'in', 'price',
  'rate', 'cost', 'how', 'much', 'for', 'of', 'the', 'a', 'an', 'what', 'can', 'i', 'get',
  'please', 'tell', 'me', 'about', 'buy', 'want', 'need', 'find', 'check',
  'ਕੀ', 'ਹੈ', 'ਮਿਲਦਾ', 'ਉਪਲਬਧ', 'ਸਟਾਕ', 'ਰੇਟ', 'ਕੀਮਤ', 'ਕਿੰਨਾ',
  'क्या', 'है', 'उपलब्ध', 'स्टॉक', 'दाम', 'कीमत', 'रेट', 'कितना', 'मिलता',
]);

const UI_COPY: Record<
  StoreLang,
  {
    fabLabel: string;
    title: string;
    placeholder: string;
    waHelp: string;
    inStock: string;
    outOfStock: string;
    addBtn: string;
    chips: Array<{ label: string; query: string }>;
  }
> = {
  en: {
    fabLabel: 'Ask Store AI',
    title: 'Store AI Helper',
    placeholder: 'Ask stock availability or store policy...',
    waHelp: 'WhatsApp Help',
    inStock: 'In Stock',
    outOfStock: 'Out of Stock',
    addBtn: 'Add',
    chips: [
      { label: 'Aashirvaad Atta', query: 'Aashirvaad Atta' },
      { label: 'Frozen Stock', query: 'Frozen Peas' },
      { label: 'Delivery Policy', query: 'Delivery & Pickup Policy' },
      { label: 'Return Policy', query: 'Return & Refund Policy' },
      { label: '12% Wholesale Policy', query: '12% Wholesale Discount Policy' },
    ],
  },
  pa: {
    fabLabel: 'ਸਟੋਰ AI ਮਦਦ',
    title: 'ਸਟੋਰ AI ਸਹਾਇਕ',
    placeholder: 'ਸਟਾਕ ਜਾਂ ਸਟੋਰ ਨੀਤੀ ਪੁੱਛੋ...',
    waHelp: 'ਵਟਸਐਪ ਮਦਦ',
    inStock: 'ਸਟਾਕ ਵਿੱਚ',
    outOfStock: 'ਸਟਾਕ ਖਤਮ',
    addBtn: 'ਸ਼ਾਮਲ',
    chips: [
      { label: 'ਆਸ਼ੀਰਵਾਦ ਆਟਾ', query: 'Aashirvaad Atta' },
      { label: '-18°C ਫਰੋਜ਼ਨ ਸਟਾਕ', query: 'Frozen Peas' },
      { label: 'ਡਿਲੀਵਰੀ ਨੀਤੀ', query: 'Delivery & Pickup Policy' },
      { label: 'ਵਾਪਸੀ ਨੀਤੀ', query: 'Return & Refund Policy' },
      { label: '12% ਛੋਟ ਨੀਤੀ', query: '12% Wholesale Discount Policy' },
    ],
  },
  hi: {
    fabLabel: 'स्टोर AI मदद',
    title: 'स्टोर AI सहायक',
    placeholder: 'स्टॉक उपलब्धता या स्टोर नीति पूछें...',
    waHelp: 'व्हाट्सएप मदद',
    inStock: 'स्टॉक में',
    outOfStock: 'स्टॉक समाप्त',
    addBtn: 'जोड़ें',
    chips: [
      { label: 'आशीर्वाद आटा', query: 'Aashirvaad Atta' },
      { label: '-18°C फ्रोजन स्टॉक', query: 'Frozen Peas' },
      { label: 'डिलीवरी नीति', query: 'Delivery & Pickup Policy' },
      { label: 'वापसी नीति', query: 'Return & Refund Policy' },
      { label: '12% छूट नीति', query: '12% Wholesale Discount Policy' },
    ],
  },
};

export const StoreAICopilot: React.FC<StoreAICopilotProps> = ({
  products,
  lang: externalLang,
  onChangeLang,
  onAddToCart,
  onSelectAisle,
}) => {
  const [open, setOpen] = useState(false);
  const [lang, setLang] = useState<StoreLang>(externalLang || 'en');
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'ai',
      text: 'Sat Sri Akal! Ask me about item availability, stock & wholesale prices, delivery/pickup, return policy, or store hours in English, ਪੰਜਾਬੀ, or हिन्दी.',
    },
  ]);

  const scrollRef = useRef<HTMLDivElement | null>(null);
  const copy = UI_COPY[lang];

  useEffect(() => {
    if (externalLang && externalLang !== lang) {
      setLang(externalLang);
    }
  }, [externalLang, lang]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, open]);

  const toggleLang = (nextLang: StoreLang) => {
    setLang(nextLang);
    onChangeLang?.(nextLang);
    const intro =
      nextLang === 'pa'
        ? 'ਸਤ ਸ੍ਰੀ ਅਕਾਲ! ਤੁਸੀਂ ਕਿਸੇ ਵੀ ਚੀਜ਼ ਦੀ ਉਪਲਬਧਤਾ, 12% ਥੋਕ ਰੇਟ, ਡਿਲੀਵਰੀ/ਪਿਕਅੱਪ, ਵਾਪਸੀ ਨੀਤੀ ਜਾਂ -18°C ਫਰੋਜ਼ਨ ਸਟਾਕ ਬਾਰੇ ਪੁੱਛ ਸਕਦੇ ਹੋ।'
        : nextLang === 'hi'
        ? 'नमस्ते! आप किसी भी उत्पाद की उपलब्धता, 12% थोक दाम, डिलीवरी/पिकअप, वापसी नीति या -18°C फ्रोजन स्टॉक के बारे में पूछ सकते हैं।'
        : 'Switched to English! Ask about any product availability, wholesale price, delivery/pickup, or 7-day return policy at Nav Grocery Store.';

    setMessages((prev) => [
      ...prev,
      {
        id: `lang-${Date.now()}`,
        sender: 'ai',
        text: intro,
      },
    ]);
  };

  const handleQuery = (queryRaw: string) => {
    const q = queryRaw.trim();
    if (!q) return;

    const userMsg: ChatMessage = { id: `u-${Date.now()}`, sender: 'user', text: q };
    const lower = q.toLowerCase();

    let replyText = '';
    let matchedProducts: Product[] = [];

    if (/return|refund|replace|exchange|damaged|expired|policy|ਵਾਪਸੀ|ਰਿਫੰਡ|ਬਦਲੀ|ਨੀਤੀ|वापसी|रिफंड|बदलाव|नीति/i.test(lower) && !/fresh|fruit|veg|deliver|pickup|discount|12%|pay|upi|cod/i.test(lower)) {
      replyText =
        lang === 'pa'
          ? 'ਵਾਪਸੀ ਅਤੇ ਬਦਲੀ ਨੀਤੀ: ਖਰਾਬ, ਗਲਤ ਜਾਂ ਘੱਟ ਮਿਲੀ ਸੀਲਬੰਦ ਵਸਤੂ ਲਈ 7 ਦਿਨਾਂ ਦੇ ਅੰਦਰ ਆਸਾਨ ਬਦਲੀ ਜਾਂ ਰਿਫੰਡ ਉਪਲਬਧ ਹੈ (ਡਿਜੀਟਲ ਬਿੱਲ ਨਾਲ)। -18°C ਫਰੋਜ਼ਨ ਸਮਾਨ ਡਿਲੀਵਰੀ ਸਮੇਂ ਹੀ ਜਾਂਚਿਆ ਜਾਂਦਾ ਹੈ। WhatsApp: +91 8847370905'
          : lang === 'hi'
          ? 'वापसी और बदलाव नीति: क्षतिग्रस्त, गलत या कम सीलबंद उत्पाद के लिए डिजिटल बिल के साथ 7 दिनों के भीतर आसान रिप्लेसमेंट या रिफंड उपलब्ध है। -18°C फ्रोजन आइटम डिलीवरी के समय सत्यापित किए जाते हैं। WhatsApp: +91 8847370905'
          : 'Return & Replacement Policy: 7-day return/instant replacement for damaged, incorrect, or short-shipped factory-sealed packs with your digital invoice (under Consumer Protection E-Commerce Rules 2020). Call/WhatsApp +91 8847370905.';
    } else if (/fresh|fruit|vegetable|sabzi|produce|ਤਾਜ਼ਾ|ਫਲ|ਸਬਜ਼ੀ|ताज़ा|फल|सब्ज़ी/i.test(lower)) {
      replyText =
        lang === 'pa'
          ? 'ਜ਼ੀਰੋ ਫਰੈਸ਼ ਪ੍ਰੋਡਿਊਸ ਨੀਤੀ: ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਸਿਰਫ਼ 100% ਫੈਕਟਰੀ-ਸੀਲਬੰਦ ਰਾਸ਼ਨ ਅਤੇ -18°C ਫਰੋਜ਼ਨ ਸਬਜ਼ੀਆਂ (Safal ਮਟਰ, ਸਵੀਟ ਕਾਰਨ, McCain) ਵੇਚਦਾ ਹੈ — ਕੋਈ ਤਾਜ਼ੇ ਫਲ ਜਾਂ ਖੁੱਲ੍ਹੀਆਂ ਸਬਜ਼ੀਆਂ ਨਹੀਂ।'
          : lang === 'hi'
          ? 'जीरो फ्रेश प्रोड्यूस नीति: नव ग्रोसरी स्टोर केवल 100% फैक्ट्री-सीलबंद किराना और -18°C फ्रोजन आइटम (Safal मटर, स्वीट कॉर्न, McCain) बेचता है — कोई ताज़ा फल या खुली सब्ज़ियां नहीं।'
          : 'Zero Fresh Produce Policy: Nav Grocery Store stocks strictly 100% factory-sealed groceries and -18°C cold-chain frozen foods (Safal Peas, Sweet Corn, McCain, Amul Paneer) — zero fresh fruits or fresh vegetables.';
    } else if (/hour|open|close|timing|time|address|location|helpline|contact|ਸਮਾਂ|ਖੁੱਲ੍ਹਾ|ਪਤਾ|ਸੰਪਰਕ|समय|खुला|पता|संपर्क/i.test(lower)) {
      replyText =
        lang === 'pa'
          ? 'ਸਟੋਰ ਦਾ ਪਤਾ ਅਤੇ ਸਮਾਂ: ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ, ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ, ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ, ਪੰਜਾਬ 148020। ਰੋਜ਼ਾਨਾ ਸਵੇਰੇ 8:00 ਤੋਂ ਰਾਤ 9:00 ਵਜੇ ਤੱਕ ਖੁੱਲ੍ਹਾ। ਹੈਲਪਲਾਈਨ: +91 8847370905 / +91 9915752905'
          : lang === 'hi'
          ? 'स्टोर का पता और समय: नव ग्रोसरी स्टोर, मेन बाजार, संधौर, जिला मलेरकोटला, पंजाब 148020। प्रतिदिन सुबह 8:00 से रात 9:00 बजे तक खुला। हेल्पलाइन: +91 8847370905 / +91 9915752905'
          : 'Store Location & Hours: Nav Grocery Store, Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab 148020. Open daily 8:00 AM – 9:00 PM. Helpline: +91 8847370905 / +91 9915752905.';
    } else if (/discount|wholesale|mrp|12%|gst|bill|invoice|ਛੋਟ|ਥੋਕ|ਬਿੱਲ|छूट|थोक|बिल/i.test(lower)) {
      replyText =
        lang === 'pa'
          ? 'ਥੋਕ ਕੀਮਤ ਨੀਤੀ: ਸਾਰੀਆਂ 308 ਪ੍ਰਮਾਣਿਤ ਵਸਤੂਆਂ ’ਤੇ MRP ਤੋਂ ਫਲੈਟ 12% ਛੋਟ ਮਿਲਦੀ ਹੈ, ਅਤੇ ਹਰ ਆਰਡਰ ਤੋਂ ਬਾਅਦ GST ਡਿਜੀਟਲ ਬਿੱਲ ਤਿਆਰ ਹੁੰਦਾ ਹੈ।'
          : lang === 'hi'
          ? 'थोक मूल्य नीति: सभी 308 प्रमाणित किराना उत्पादों पर MRP से फ्लैट 12% छूट लागू है, और हर ऑर्डर के बाद आधिकारिक GST डिजिटल बिल मिलता है।'
          : 'Wholesale Pricing Policy: Flat 12% off MRP on all 308 verified SKUs automatically (Sale = round(MRP × 0.88)), with an itemized post-order digital GST receipt.';
    } else if (
      lower.includes('delivery') ||
      lower.includes('pickup') ||
      lower.includes('ਡਿਲੀਵਰੀ') ||
      lower.includes('ਪਿਕਅੱਪ') ||
      lower.includes('डिलीवरी') ||
      lower.includes('पिकअप')
    ) {
      replyText =
        lang === 'pa'
          ? 'ਡਿਲੀਵਰੀ ਅਤੇ ਪਿਕਅੱਪ ਨੀਤੀ: ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ (148020) ਵਿੱਚ 30 ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਸਟੋਰ ਪਿਕਅੱਪ ਅਤੇ 2 ਘੰਟੇ ਲੋਕਲ ਡਿਲੀਵਰੀ। ₹499 ਤੋਂ ਵੱਧ ਆਰਡਰ ’ਤੇ ਡਿਲੀਵਰੀ ਮੁਫ਼ਤ ਹੈ (ਨਹੀਂ ਤਾਂ ₹30)।'
          : lang === 'hi'
          ? 'डिलीवरी और पिकअप नीति: मेन बाजार, संधौर (148020) में 30 मिनट एक्सप्रेस स्टोर पिकअप और 2 घंटे लोकल डिलीवरी। ₹499 से अधिक के ऑर्डर पर डिलीवरी मुफ्त है (अन्यथा ₹30)।'
          : 'Delivery & Pickup Policy: 30-min Express Store Pickup at Main Bazaar, Sandhaur 148020 and 2-hour local delivery. Free delivery on orders ₹499+ (₹30 below ₹499).';
    } else if (lower.includes('upi') || lower.includes('pay') || lower.includes('cod') || lower.includes('qr')) {
      replyText =
        lang === 'pa'
          ? 'ਭੁਗਤਾਨ ਨੀਤੀ: ਤੁਸੀਂ Dynamic UPI QR ਕੋਡ, UPI ਐਪ, ਜਾਂ ₹199+ ਦੇ ਆਰਡਰ ’ਤੇ Cash on Delivery (COD) / ਸਟੋਰ ਪਿਕਅੱਪ ਸਮੇਂ ਭੁਗਤਾਨ ਕਰ ਸਕਦੇ ਹੋ।'
          : lang === 'hi'
          ? 'भुगतान नीति: आप डायनेमिक UPI QR कोड, UPI ऐप, या ₹199+ के ऑर्डर पर कैश ऑन डिलीवरी (COD) / स्टोर पिकअप पर भुगतान कर सकते हैं।'
          : 'Payment Policy: Pay via Dynamic UPI QR Code (PhonePe/GPay/Paytm), UPI intent, or Cash on Delivery / Pay at Pickup (COD available on orders ₹199+).';
    } else if (
      lower.includes('frozen') ||
      lower.includes('peas') ||
      lower.includes('mccain') ||
      lower.includes('safal') ||
      lower.includes('paneer') ||
      lower.includes('ਫਰੋਜ਼ਨ') ||
      lower.includes('फ्रोजन')
    ) {
      matchedProducts = products
        .filter(
          (p) =>
            (p.department + p.category + p.name).toLowerCase().includes('frozen') ||
            /safal|mccain|paneer/i.test(p.name)
        )
        .slice(0, 4);
      onSelectAisle('frozen');
      replyText =
        lang === 'pa'
          ? 'ਸਾਡੇ ਕੋਲ -18°C ਕੋਲਡ-ਚੇਨ ਵਿੱਚ ਸਫਲ ਮਟਰ, ਮੈਕਕੇਨ ਫ੍ਰਾਈਜ਼ ਅਤੇ ਅਮੁਲ ਪਨੀਰ ਉਪਲਬਧ ਹਨ (12% ਛੋਟ):'
          : lang === 'hi'
          ? 'हमारे पास -18°C कोल्ड-चेन में सफल मटर, मैक्केन फ्राइज़ और अमूल पनीर उपलब्ध हैं (12% छूट):'
          : 'Live -18°C Cold-Chain Frozen stock (all flat 12% off MRP):';
    } else {
      const cleanedTokens = lower
        .replace(/[?.,!।]+/g, ' ')
        .split(/\s+/)
        .map((t) => t.trim())
        .filter((t) => t.length >= 2 && !STOP_WORDS.has(t));

      const tokens = cleanedTokens.length > 0 ? cleanedTokens : lower.split(/\s+/).filter((t) => t.length >= 2);

      matchedProducts = products
        .filter((p) => {
          const hay = `${p.id} ${p.brand} ${p.name} ${p.category}`.toLowerCase();
          return tokens.every((tk) => hay.includes(tk));
        })
        .slice(0, 4);

      if (matchedProducts.length === 0 && tokens.length > 0) {
        matchedProducts = products
          .filter((p) => {
            const hay = `${p.id} ${p.brand} ${p.name} ${p.category}`.toLowerCase();
            return tokens.some((tk) => hay.includes(tk));
          })
          .slice(0, 4);
      }

      if (matchedProducts.length > 0) {
        const inStockCount = matchedProducts.filter((p) => Number(p.stock) > 0).length;
        replyText =
          lang === 'pa'
            ? `"${q}" ਲਈ ${matchedProducts.length} ਉਤਪਾਦ ਮਿਲੇ (${inStockCount} ਸਟਾਕ ਵਿੱਚ, MRP ਤੋਂ 12% ਛੋਟ):`
            : lang === 'hi'
            ? `"${q}" के लिए ${matchedProducts.length} उत्पाद मिले (${inStockCount} स्टॉक में, MRP से 12% छूट):`
            : `Found ${matchedProducts.length} matching items (${inStockCount} in stock at Main Bazaar, 12% off MRP):`;
      } else {
        replyText =
          lang === 'pa'
            ? `"${q}" ਨਹੀਂ ਮਿਲਿਆ। ਬ੍ਰਾਂਡ (Aashirvaad, Tata, Amul, Verka, MDH), ਸਟੋਰ ਨੀਤੀ (Return, Delivery, Hours) ਪੁੱਛੋ ਜਾਂ WhatsApp +91 8847370905 ਤੇ ਸੰਪਰਕ ਕਰੋ।`
            : lang === 'hi'
            ? `"${q}" नहीं मिला। कृपया ब्रांड (Aashirvaad, Tata, Amul, Verka, MDH) या स्टोर नीति (Return, Delivery, Hours) पूछें, या WhatsApp +91 8847370905 पर संपर्क करें।`
            : `No direct match for "${q}". Try brand names (Aashirvaad, India Gate, Verka, Amul, MDH), ask about Return/Delivery/Wholesale Policy, or message WhatsApp +91 8847370905.`;
      }
    }

    const aiMsg: ChatMessage = {
      id: `a-${Date.now() + 1}`,
      sender: 'ai',
      text: replyText,
      matches: matchedProducts.length > 0 ? matchedProducts : undefined,
    };

    setMessages((prev) => [...prev, userMsg, aiMsg]);
    setInput('');
  };

  return (
    <div className="fixed bottom-4 right-4 z-40 no-print">
      {open ? (
        <div
          role="dialog"
          aria-label={copy.title}
          className="w-[340px] sm:w-[385px] bg-white rounded-2xl shadow-2xl border border-[#0F3D28]/20 overflow-hidden flex flex-col max-h-[560px]"
        >
          <div className="px-4 py-3 bg-[#0F3D28] text-white flex items-center justify-between gap-2 shrink-0">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-8 h-8 rounded-lg bg-[#E6A817] text-[#14221B] flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <h3 className="text-sm font-bold leading-tight truncate">{copy.title}</h3>
                <p className="text-[11px] text-white/75">EN · ਪੰਜਾਬੀ · हिन्दी</p>
              </div>
            </div>

            <div className="flex items-center gap-1 shrink-0">
              <div className="flex rounded-lg bg-white/10 p-0.5 text-[11px] font-semibold">
                <button
                  type="button"
                  onClick={() => toggleLang('en')}
                  className={`px-2 py-0.5 rounded-md cursor-pointer ${
                    lang === 'en' ? 'bg-[#E6A817] text-[#14221B]' : 'text-white'
                  }`}
                >
                  EN
                </button>
                <button
                  type="button"
                  onClick={() => toggleLang('pa')}
                  className={`px-2 py-0.5 rounded-md cursor-pointer ${
                    lang === 'pa' ? 'bg-[#E6A817] text-[#14221B]' : 'text-white'
                  }`}
                >
                  ਪੰਜਾਬੀ
                </button>
                <button
                  type="button"
                  onClick={() => toggleLang('hi')}
                  className={`px-2 py-0.5 rounded-md cursor-pointer ${
                    lang === 'hi' ? 'bg-[#E6A817] text-[#14221B]' : 'text-white'
                  }`}
                >
                  हिन्दी
                </button>
              </div>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Close Store AI"
                className="p-1 rounded-lg hover:bg-white/15 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div ref={scrollRef} className="flex-1 overflow-y-auto p-3.5 space-y-3 bg-[#F8F7F2]">
            {messages.map((m) => (
              <div
                key={m.id}
                className={`flex flex-col ${m.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[90%] px-3.5 py-2.5 rounded-xl text-xs leading-relaxed ${
                    m.sender === 'user'
                      ? 'bg-[#0F3D28] text-white'
                      : 'bg-white text-[#14221B] border border-[#0F3D28]/10'
                  }`}
                >
                  {m.text}
                </div>
                {m.matches && (
                  <div className="w-full mt-2 space-y-1.5">
                    {m.matches.map((item) => {
                      const inStock = Number(item.stock) > 0;
                      return (
                        <div
                          key={item.id}
                          className="flex items-center justify-between gap-2 p-2 rounded-xl bg-white border border-[#0F3D28]/12 text-xs"
                        >
                          <div className="min-w-0">
                            <p className="font-semibold text-[#14221B] truncate">{item.name}</p>
                            <div className="flex flex-wrap items-center gap-1.5 text-[11px] text-[#526058] tabular-nums mt-0.5">
                              <span className="font-bold text-[#0F3D28]">₹{item.sale}</span>
                              <span className="line-through">₹{item.mrp}</span>
                              <span>·</span>
                              <span
                                className={`font-semibold ${
                                  inStock ? 'text-[#0F3D28]' : 'text-red-700'
                                }`}
                              >
                                {inStock ? `${copy.inStock} (${item.stock})` : copy.outOfStock}
                              </span>
                            </div>
                          </div>
                          <button
                            type="button"
                            disabled={!inStock}
                            onClick={() => onAddToCart(item)}
                            className="px-2.5 py-1.5 rounded-lg bg-[#0F3D28] text-white font-semibold text-[11px] inline-flex items-center gap-1 shrink-0 hover:bg-[#0A2A1B] cursor-pointer disabled:opacity-45 disabled:cursor-not-allowed"
                          >
                            <Plus className="w-3 h-3 text-[#E6A817]" />
                            {copy.addBtn}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="px-3 py-2 bg-white border-t border-[#0F3D28]/10 flex flex-wrap gap-1.5 shrink-0">
            {copy.chips.map((chip) => (
              <button
                key={chip.label}
                type="button"
                onClick={() => handleQuery(chip.query)}
                className="px-2.5 py-1 rounded-lg bg-[#F8F7F2] hover:bg-[#EFECE2] text-[11px] font-medium text-[#0F3D28] cursor-pointer"
              >
                {chip.label}
              </button>
            ))}
            <a
              href="https://wa.me/918847370905"
              target="_blank"
              rel="noreferrer"
              className="px-2.5 py-1 rounded-lg bg-[#0F3D28]/10 text-[11px] font-semibold text-[#0F3D28] inline-flex items-center gap-1"
            >
              <MessageCircle className="w-3 h-3" />
              {copy.waHelp}
            </a>
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleQuery(input);
            }}
            className="p-2.5 bg-white border-t border-[#0F3D28]/10 flex items-center gap-2 shrink-0"
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={copy.placeholder}
              className="flex-1 px-3 py-2 rounded-xl bg-[#F8F7F2] text-xs focus:outline-none focus:ring-1 focus:ring-[#0F3D28]"
            />
            <button
              type="submit"
              aria-label="Send message"
              className="p-2 rounded-xl bg-[#0F3D28] text-white hover:bg-[#0A2A1B] cursor-pointer"
            >
              <Send className="w-4 h-4 text-[#E6A817]" />
            </button>
          </form>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setOpen(true)}
          aria-label={copy.fabLabel}
          className="inline-flex items-center gap-2 px-4 py-3 rounded-full bg-[#0F3D28] text-white shadow-xl border border-[#E6A817]/50 hover:bg-[#0A2A1B] transition-transform hover:-translate-y-0.5 cursor-pointer"
        >
          <Bot className="w-4 h-4 text-[#E6A817]" />
          <span className="text-xs font-bold whitespace-nowrap">{copy.fabLabel}</span>
        </button>
      )}
    </div>
  );
};
