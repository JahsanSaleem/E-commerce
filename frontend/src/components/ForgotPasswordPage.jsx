import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { requestPasswordReset, resetPassword } from "../services/authService.js";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const remaining = challenge ? Math.max(0, Math.ceil((Date.parse(challenge.resendAt) - now) / 1000)) : 0;
  function showError(failure) {
    const data = failure.response?.data;
    setError(data?.message || Object.values(data || {})[0] || "Unable to reset password. Please try again.");
  }
  async function sendCode() {
    setBusy(true); setError(""); setNotice("");
    try {
      const result = await requestPasswordReset(email.trim());
      setChallenge(result); setNow(Date.now()); setCode(""); setNotice(result.message);
    } catch (failure) { showError(failure); }
    finally { setBusy(false); }
  }
  async function submit(event) {
    event.preventDefault();
    if (!challenge) { await sendCode(); return; }
    if (password !== confirmation) { setError("Passwords do not match."); return; }
    if (new TextEncoder().encode(password).length > 72) { setError("Password must not exceed 72 UTF-8 bytes."); return; }
    setBusy(true); setError("");
    try {
      await resetPassword({ resetId: challenge.resetId, code, password });
      setPassword(""); setConfirmation("");
      // Reset expires existing server sessions; reload to refresh the auth context too.
      window.location.replace("/login?password-reset=success");
    } catch (failure) { showError(failure); }
    finally { setBusy(false); }
  }
  return <section className="auth-section mx-auto max-w-md">
    <div className="auth-panel panel overflow-hidden">
      <div className="auth-heading text-white">
        <p className="eyebrow text-orange-400">Account recovery</p>
        <h1 className="mt-2 text-3xl font-bold">{challenge ? "Reset your password" : "Forgot password?"}</h1>
        <p className="mt-2 text-sm text-slate-300">{challenge ? "Enter your email code and choose a new password." : "Enter your account email to request a reset code."}</p>
      </div>
      <form className="space-y-5 p-8" onSubmit={submit}>
        {error && <p role="alert" className="bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}
        {notice && <p role="status" className="bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
        {challenge ? <>
          <p className="text-sm break-words">Check the inbox for <strong>{email}</strong>. Google accounts should use Google sign-in.</p>
          <div><label className="field-label" htmlFor="reset-code">Six-digit reset code</label>
            <input id="reset-code" className="field-input" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required autoFocus value={code} onChange={event => setCode(event.target.value.replace(/[^0-9]/g, ""))} /></div>
          <div><label className="field-label" htmlFor="reset-password">New password</label>
            <input id="reset-password" className="field-input" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={password} onChange={event => setPassword(event.target.value)} />
            <p className="mt-1 text-xs text-slate-500">Use 8–72 characters.</p></div>
          <div><label className="field-label" htmlFor="reset-confirmation">Confirm new password</label>
            <input id="reset-confirmation" className="field-input" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={confirmation} onChange={event => setConfirmation(event.target.value)} /></div>
          <p className="text-xs text-slate-500">{now >= Date.parse(challenge.expiresAt) ? "Code expired. Request a new code below." : "Your code expires five minutes after it is sent."}</p>
        </> : <div><label className="field-label" htmlFor="reset-email">Email</label>
          <input id="reset-email" className="field-input" type="email" autoComplete="email" maxLength={255} required autoFocus value={email} onChange={event => setEmail(event.target.value)} /></div>}
        <button className="btn-primary w-full py-3" disabled={busy}>{busy ? "Please wait…" : challenge ? "Update password" : "Send reset code"}</button>
        {challenge && <>
          <button type="button" className="btn-outline w-full" disabled={busy || remaining > 0} onClick={sendCode}>{remaining > 0 ? `Resend code in ${remaining}s` : "Resend code"}</button>
          <button type="button" className="text-sm text-orange-700 underline" disabled={busy} onClick={() => { setChallenge(null); setCode(""); setPassword(""); setConfirmation(""); setError(""); setNotice(""); }}>Use a different email</button>
        </>}
        <p className="text-center text-sm"><Link to="/login" className="font-semibold text-orange-700 hover:underline">Back to sign in</Link></p>
      </form>
    </div>
  </section>;
}
