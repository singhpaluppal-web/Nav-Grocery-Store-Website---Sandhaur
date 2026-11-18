import crypto from 'crypto';
export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { email, otp, token } = req.body || {};
  const cleanEmail = String(email || '').trim().toLowerCase();
  const cleanOtp = String(otp || '').trim();
  const secret = process.env.OTP_SECRET || 'NavStore_905_Secure_Key_2026';
  try {
    const decoded = JSON.parse(Buffer.from(String(token || ''), 'base64url').toString('utf8'));
    if (decoded.email !== cleanEmail) return res.status(400).json({ error: 'Email mismatch.' });
    if (Date.now() > Number(decoded.exp)) return res.status(400).json({ error: 'OTP expired. Request a new code.' });
    const expected = crypto.createHmac('sha256', secret).update(`${cleanEmail}:${cleanOtp}:${decoded.exp}`).digest('hex');
    if (expected !== decoded.sig) return res.status(400).json({ error: 'Invalid 6-digit OTP code.' });
    return res.status(200).json({ ok: true, email: cleanEmail });
  } catch {
    return res.status(400).json({ error: 'Invalid verification session.' });
  }
}
