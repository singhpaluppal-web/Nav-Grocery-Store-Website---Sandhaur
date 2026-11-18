import React, { useEffect, useRef, useState } from 'react';
import { Camera, ScanBarcode, Search, X, CheckCircle2, AlertCircle } from 'lucide-react';
import { Product } from '../types';
import { resolveProductImage } from '../utils/brandPhotos';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  products: Product[];
  onClose: () => void;
  onSelectProduct: (product: Product) => void;
}

interface BarcodeDetectorLike {
  detect: (source: ImageBitmapSource) => Promise<Array<{ rawValue?: string }>>;
}

declare global {
  interface Window {
    BarcodeDetector?: new (opts?: { formats?: string[] }) => BarcodeDetectorLike;
  }
}

export const BarcodeScannerModal: React.FC<BarcodeScannerModalProps> = ({
  isOpen,
  products,
  onClose,
  onSelectProduct,
}) => {
  const [codeInput, setCodeInput] = useState('');
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState('');
  const [matchedProduct, setMatchedProduct] = useState<Product | null>(null);
  const [notFoundMsg, setNotFoundMsg] = useState('');

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);

  const stopCamera = () => {
    if (rafRef.current !== null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      setMatchedProduct(null);
      setNotFoundMsg('');
      setCameraError('');
    }
    return () => stopCamera();
  }, [isOpen]);

  const lookupBarcodeOrSku = (raw: string) => {
    const clean = raw.trim().toUpperCase();
    setNotFoundMsg('');
    if (!clean) return;
    const found = products.find(
      (p) =>
        p.id.toUpperCase() === clean ||
        (p.barcode && p.barcode.trim().toUpperCase() === clean) ||
        p.name.toUpperCase().includes(clean)
    );
    if (found) {
      setMatchedProduct(found);
      stopCamera();
    } else {
      setMatchedProduct(null);
      setNotFoundMsg(`No catalog match found for "${clean}". Try SKU ID (e.g. NAV081) or 13-digit EAN barcode.`);
    }
  };

  const startCameraScan = async () => {
    setCameraError('');
    setNotFoundMsg('');
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError('Camera access is not supported in this browser context. Enter a barcode or SKU manually below.');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });
      streamRef.current = stream;
      setCameraActive(true);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }

      if (window.BarcodeDetector) {
        const detector = new window.BarcodeDetector({
          formats: ['ean_13', 'ean_8', 'upc_a', 'code_128', 'qr_code'],
        });
        const tick = async () => {
          if (!videoRef.current || !streamRef.current) return;
          try {
            const barcodes = await detector.detect(videoRef.current);
            if (barcodes.length > 0 && barcodes[0].rawValue) {
              const rawVal = barcodes[0].rawValue;
              setCodeInput(rawVal);
              lookupBarcodeOrSku(rawVal);
              return;
            }
          } catch {
            // keep scanning
          }
          rafRef.current = requestAnimationFrame(tick);
        };
        rafRef.current = requestAnimationFrame(tick);
      } else {
        setCameraError('Live camera feed active — automatic hardware barcode decoding requires Chrome/Android; type the printed barcode below.');
      }
    } catch {
      setCameraError('Camera permission denied or unavailable. Use manual barcode/SKU lookup below.');
      setCameraActive(false);
    }
  };

  if (!isOpen) return null;

  const quickSamples = ['NAV001', 'NAV081', 'NAV151', 'NK309']
    .map((id) => products.find((p) => p.id === id))
    .filter((p): p is Product => Boolean(p));

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Barcode and SKU Scanner"
    >
      <div
        className="w-full max-w-md bg-[#F8F7F2] rounded-2xl border border-[#0F3D28]/15 shadow-2xl overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <ScanBarcode className="w-5 h-5 text-[#E6A817]" />
            <div>
              <h2 className="font-display font-bold text-base">Barcode &amp; SKU Scanner</h2>
              <p className="text-[11px] text-white/75">Scan pack EAN-13 or enter SKU ID</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close barcode scanner"
            className="p-1.5 rounded-lg hover:bg-white/15 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          {/* Camera Preview or Trigger */}
          <div className="rounded-xl border border-[#0F3D28]/15 bg-white overflow-hidden">
            {cameraActive ? (
              <div className="relative bg-black aspect-video flex items-center justify-center">
                <video
                  ref={videoRef}
                  playsInline
                  muted
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-x-8 top-1/2 -translate-y-1/2 h-0.5 bg-[#E6A817]" />
                <button
                  type="button"
                  onClick={stopCamera}
                  className="absolute bottom-2.5 right-2.5 px-3 py-1 rounded-lg bg-black/75 text-white text-xs font-semibold cursor-pointer"
                >
                  Stop Camera
                </button>
              </div>
            ) : (
              <div className="p-4 flex items-center justify-between gap-3">
                <div className="text-xs text-[#526058]">
                  <p className="font-bold text-[#14221B]">Use Device Camera</p>
                  <p>Point at any sealed pack barcode or shelf QR tag.</p>
                </div>
                <button
                  type="button"
                  onClick={startCameraScan}
                  className="px-3.5 py-2 rounded-lg bg-[#0F3D28] hover:bg-[#0A2A1B] text-white text-xs font-bold inline-flex items-center gap-1.5 shrink-0 cursor-pointer"
                >
                  <Camera className="w-3.5 h-3.5 text-[#E6A817]" />
                  <span>Open Camera</span>
                </button>
              </div>
            )}
          </div>

          {cameraError && (
            <div className="px-3 py-2 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{cameraError}</span>
            </div>
          )}

          {/* Manual Lookup Form */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              lookupBarcodeOrSku(codeInput);
            }}
            className="flex gap-2"
          >
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={codeInput}
                onChange={(e) => setCodeInput(e.target.value)}
                placeholder="Enter Barcode or SKU (e.g. NAV081)"
                className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-white border border-[#0F3D28]/20 text-xs sm:text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-2.5 rounded-xl bg-[#E6A817] hover:bg-[#f0b72b] text-[#14221B] text-xs font-bold whitespace-nowrap cursor-pointer"
            >
              Lookup
            </button>
          </form>

          {notFoundMsg && (
            <p className="text-xs font-semibold text-red-700">{notFoundMsg}</p>
          )}

          {matchedProduct && (
            <div className="bg-white rounded-xl p-3.5 border border-[#0F3D28] flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <img
                  src={resolveProductImage(matchedProduct)}
                  alt={matchedProduct.name}
                  className="w-12 h-12 rounded-lg object-contain bg-[#F8F7F2] shrink-0"
                />
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5 text-[11px] font-bold text-[#0F3D28]">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span className="font-mono-num">{matchedProduct.id}</span>
                  </div>
                  <p className="text-xs font-bold text-[#14221B] truncate">{matchedProduct.name}</p>
                  <p className="text-xs font-mono-num text-[#0F3D28] font-bold">
                    ₹{matchedProduct.sale}{' '}
                    <span className="text-[#526058] line-through font-normal">
                      ₹{matchedProduct.mrp}
                    </span>
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  onSelectProduct(matchedProduct);
                  onClose();
                }}
                className="px-3 py-2 rounded-lg bg-[#0F3D28] text-white text-xs font-bold shrink-0 cursor-pointer"
              >
                Open Item
              </button>
            </div>
          )}

          {quickSamples.length > 0 && (
            <div className="space-y-1.5">
              <p className="text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                Quick Shelf Scan Samples
              </p>
              <div className="flex flex-wrap gap-1.5">
                {quickSamples.map((sample) => (
                  <button
                    key={sample.id}
                    type="button"
                    onClick={() => {
                      setCodeInput(sample.barcode || sample.id);
                      lookupBarcodeOrSku(sample.id);
                    }}
                    className="px-2.5 py-1.5 rounded-lg bg-white border border-[#0F3D28]/15 hover:border-[#0F3D28] text-[11px] font-semibold text-[#14221B] cursor-pointer"
                  >
                    {sample.id} · {sample.brand}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
