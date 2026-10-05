import GoogleSignIn from "./GoogleSignIn.jsx";
import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/authContext.js";

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const googleError = new URLSearchParams(location.search).get("google");
  const [error, setError] = useState(googleError === "existing-account"
    ? "This email already has a store account. Sign in with your existing email and password."
    : googleError === "failed" ? "Google sign-in could not be completed. Please try again." : "");
  const [submitting, setSubmitting] = useState(false);

  const destination = (account) => account.role === "ADMIN" ? "/admin/products" : account.role === "STAFF" ? "/staff/orders" : "/products";
  if (user) return <Navigate to={destination(user)} replace />;

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const account = await login(form);
      navigate(location.state?.from || destination(account), { replace: true });
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Unable to sign in.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-section mx-auto max-w-md">
      <div className="auth-panel panel overflow-hidden">
        <div className="auth-heading text-white">
          <p className="eyebrow text-orange-400">Store account</p>
          <h1 className="mt-2 text-3xl font-bold">Welcome back</h1>
          <p className="mt-2 text-sm text-slate-300">Sign in to shop or access your store workspace.</p>
        </div>
        <form className="space-y-5 p-8" onSubmit={submit}>
          {(location.state?.passwordReset || new URLSearchParams(location.search).get("password-reset") === "success") && <p role="status" className="bg-green-50 p-3 text-sm text-green-800">Password updated. Sign in with your new password.</p>}
          {error && <p role="alert" className="rounded-md bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}
          <div>
            <label className="field-label" htmlFor="login-email">Email</label>
            <input id="login-email" className="field-input" type="email" autoComplete="email" required autoFocus
              value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
          </div>
          <div>
            <label className="field-label" htmlFor="login-password">Password</label>
            <input id="login-password" className="field-input" type="password" autoComplete="current-password" required
              value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
          </div>
          <button className="btn-primary w-full py-3" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign In"}
          </button>
          <Link to="/forgot-password" className="block text-center text-sm font-semibold text-orange-700 hover:underline">Forgot password?</Link>
          <GoogleSignIn />
          <p className="text-center text-sm text-slate-500">
            New to Mustafa Hardware? <Link className="font-bold text-orange-700 hover:underline" to="/register">Create an account</Link>
          </p>
        </form>
      </div>
    </section>
  );
}
