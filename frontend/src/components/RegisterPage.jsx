import { resendRegistration } from "../services/authService.js";
import GoogleSignIn from "./GoogleSignIn.jsx";
import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/authContext.js";

export default function RegisterPage() {
  const { user, register, verifyRegistration } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [challenge, setChallenge] = useState(() => {
    try {
      const saved = JSON.parse(sessionStorage.getItem("registrationChallenge") || "null");
      return saved?.registrationId && saved?.email && saved?.resendAt ? saved : null;
    } catch { return null; }
  });
  const [code, setCode] = useState("");
  const [notice, setNotice] = useState("");
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  useEffect(() => {
    try {
      if (challenge) sessionStorage.setItem("registrationChallenge", JSON.stringify(challenge));
      else sessionStorage.removeItem("registrationChallenge");
    } catch { /* Verification still works when browser storage is unavailable. */ }
  }, [challenge]);
  const remaining = challenge ? Math.max(0, Math.ceil((Date.parse(challenge.resendAt) - now) / 1000)) : 0;
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to="/products" replace />;

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      if (challenge) {
        await verifyRegistration({ registrationId: challenge.registrationId, code });
        setChallenge(null);
        navigate("/products", { replace: true });
      } else {
        setChallenge(await register(form));
        setForm({ name: "", email: "", password: "" });
        setNotice("We sent a verification code to your email. Check your spam folder too.");
      }
    } catch (requestError) {
      const response = requestError.response?.data;
      setError(response?.message || Object.values(response || {})[0] || "Unable to create account.");
    } finally {
      setSubmitting(false);
    }
  }

  async function resend() {
    setSubmitting(true); setError(""); setNotice("");
    try {
      setChallenge(await resendRegistration(challenge.registrationId));
      setCode(""); setNotice("A new code has been sent. Use the latest code in your inbox.");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Unable to resend the code.");
    } finally { setSubmitting(false); }
  }
  const change = (event) => setForm({ ...form, [event.target.name]: event.target.value });

  return (
    <section className="auth-section mx-auto max-w-md">
      <div className="auth-panel panel overflow-hidden">
        <div className="auth-heading text-white">
          <p className="eyebrow text-orange-400">Customer account</p>
          <h1 className="mt-2 text-3xl font-bold">{challenge ? "Verify your email" : "Create your account"}</h1>
          <p className="mt-2 text-sm text-slate-300">{challenge ? "Enter the code to finish creating your account." : "Register to start shopping and track your orders."}</p>
        </div>
        <form className="space-y-5 p-8" onSubmit={submit}>
          {error && <p role="alert" className="rounded-md bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}
          {notice && <p role="status" className="bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
          {challenge ? <div className="space-y-4">
            <p className="text-sm break-words">Code sent to <strong>{challenge.email}</strong>.</p>
            <div><label className="field-label" htmlFor="registration-code">Six-digit verification code</label>
              <input id="registration-code" className="field-input" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required autoFocus value={code} onChange={event => setCode(event.target.value.replace(/[^0-9]/g, ""))} /></div>
            <p className="text-xs text-slate-500">{now >= Date.parse(challenge.expiresAt) ? "Your code has expired. Request a new code below." : "The code expires five minutes after it is sent."}</p>
          </div> : <>
          <div>
            <label className="field-label" htmlFor="register-name">Full name</label>
            <input id="register-name" name="name" className="field-input" autoComplete="name" required autoFocus value={form.name} onChange={change} />
          </div>
          <div>
            <label className="field-label" htmlFor="register-email">Email</label>
            <input id="register-email" name="email" className="field-input" type="email" autoComplete="email" required value={form.email} onChange={change} />
          </div>
          <div>
            <label className="field-label" htmlFor="register-password">Password</label>
            <input id="register-password" name="password" className="field-input" type="password" minLength="8" autoComplete="new-password" required value={form.password} onChange={change} />
            <p className="mt-1 text-xs text-slate-500">Use at least 8 characters.</p>
          </div>
          </>}
          <button className="btn-primary w-full py-3" disabled={submitting}>
            {submitting ? "Please wait…" : challenge ? "Verify & create account" : "Send verification code"}
          </button>
          {challenge ? <div className="space-y-3">
            <button type="button" className="btn-outline w-full" disabled={submitting || remaining > 0} onClick={resend}>{remaining > 0 ? `Resend code in ${remaining}s` : "Resend code"}</button>
            <button type="button" className="text-sm text-orange-700 underline" disabled={submitting} onClick={() => { setChallenge(null); setCode(""); setError(""); setNotice(""); }}>Use a different email / start again</button>
          </div> : <GoogleSignIn />}
          <p className="text-center text-sm text-slate-500">
            Already have an account? <Link className="font-bold text-orange-700 hover:underline" to="/login">Sign in</Link>
          </p>
        </form>
      </div>
    </section>
  );
}
