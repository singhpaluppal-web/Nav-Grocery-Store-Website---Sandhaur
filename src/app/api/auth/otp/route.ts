import { NextRequest, NextResponse } from 'next/server';

const otpMemoryStore = new Map<string, { code: string; expiresAt: number }>();

export async function POST(req: NextRequest) {
  try {
    const { phone, action, enteredOtp } = await req.json();
    const cleanPhone = String(phone || '').replace(/\D/g, '').slice(-10);

    if (cleanPhone.length !== 10) {
      return NextResponse.json({ success: false, message: 'Invalid 10-digit phone' }, { status: 400 });
    }

    if (action === 'SEND') {
      const code = Math.floor(100000 + Math.random() * 900000).toString();
      otpMemoryStore.set(cleanPhone, {
        code,
        expiresAt: Date.now() + 5 * 60 * 1000,
      });

      const waToken = process.env.WHATSAPP_CLOUD_API_TOKEN;
      const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;

      if (waToken && phoneNumberId) {
        await fetch(`https://graph.facebook.com/v21.0/${phoneNumberId}/messages`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${waToken}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            messaging_product: 'whatsapp',
            to: `91${cleanPhone}`,
            type: 'template',
            template: {
              name: 'nav_store_otp',
              language: { code: 'en' },
              components: [
                { type: 'body', parameters: [{ type: 'text', text: code }] },
              ],
            },
          }),
        });
      }

      return NextResponse.json({ success: true, message: 'OTP sent via WhatsApp' });
    }

    if (action === 'VERIFY') {
      const record = otpMemoryStore.get(cleanPhone);
      if (!record || Date.now() > record.expiresAt || record.code !== String(enteredOtp).trim()) {
        return NextResponse.json({ success: false, message: 'Invalid or expired OTP' }, { status: 401 });
      }
      otpMemoryStore.delete(cleanPhone);
      return NextResponse.json({ success: true, verified: true, phone: cleanPhone });
    }

    return NextResponse.json({ success: false }, { status: 400 });
  } catch {
    return NextResponse.json({ success: false }, { status: 500 });
  }
}
