import crypto from 'crypto';
import nodemailer from 'nodemailer';
export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { email } = req.body || {};
  const cleanEmail = String(email || '').trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) return res.status(400).json({ error: 'Please enter a valid Gmail address.' });
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  const secret = process.env.OTP_SECRET || 'NavStore_905_Secure_Key_2026';
  if (!user || !pass) return res.status(500).json({ error: 'Email OTP service not configured on Vercel.' });
  const otp = String(crypto.randomInt(100000, 999999));
  const exp = Date.now() + 5 * 60 * 1000;
  const sig = crypto.createHmac('sha256', secret).update(`${cleanEmail}:${otp}:${exp}`).digest('hex');
  const token = Buffer.from(JSON.stringify({ email: cleanEmail, exp, sig })).toString('base64url');
  try {
    const transporter = nodemailer.createTransport({ service: 'gmail', auth: { user, pass } });
    await transporter.sendMail({
      from: `${q}Nav Grocery Store & NavKart${q} <${user}>`,
      to: cleanEmail,
      subject: `${otp} is your Nav Grocery Store verification code`,
      html: `<div style=${q}font-family:sans-serif;max-width:440px;margin:auto;padding:24px;border:1px solid #0F3D28;border-radius:12px${q}><h2 style=${q}color:#0F3D28;margin-top:0${q}>Nav Grocery Store &amp; NavKart</h2><p>Your 6-digit sign-in OTP is:</p><div style=${q}font-size:28px;font-weight:bold;letter-spacing:6px;color:#0F3D28;background:#F8F7F2;padding:12px 16px;border-radius:8px;text-align:center${q}>${otp}</div><p style=${q}font-size:12px;color:#555;margin-top:16px${q}>Valid for 5 minutes. Main Bazaar, Sandhaur (148020).</p></div>`
    });
    return res.status(200).json({ ok: true, token });
  } catch {
    return res.status(500).json({ error: 'Failed to send OTP email. Check Gmail App Password.' });
  }
}
