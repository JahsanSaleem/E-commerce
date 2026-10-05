import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../auth/authContext.js";
import { getUsers, createUser, updateUserRole } from "../services/userService.js";

function RoleEditor({ user }) {
  const client = useQueryClient();
  const { refresh } = useAuth();
  const [role, setRole] = useState(user.role);
  const mutation = useMutation({ mutationFn: updateUserRole, onSuccess: async () => {
    await client.invalidateQueries({ queryKey: ["users"] });
    await refresh();
  }});
  return <form className="space-y-2" onSubmit={(event) => { event.preventDefault(); mutation.mutate({ userId: user.id, role }); }}>
    <select aria-label={`Role for ${user.email}`} className="field-input" value={role} onChange={(event) => { setRole(event.target.value); mutation.reset(); }} disabled={mutation.isPending}>{["CUSTOMER", "STAFF", "ADMIN"].map((value) => <option key={value}>{value}</option>)}</select>
    <button className="btn-outline" disabled={mutation.isPending || role === user.role}>{mutation.isPending ? "Saving…" : "Save role"}</button>
    {mutation.isError && <p role="alert" className="field-error">{mutation.error.response?.data?.message || "Unable to update role."}</p>}
  </form>;
}

export default function UserManagement() {
  const client = useQueryClient();
  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({ name: "", email: "", password: "", role: "STAFF" });
  const users = useQuery({ queryKey: ["users"], queryFn: getUsers });
  const create = useMutation({ mutationFn: createUser, onSuccess: async () => {
    setMessage("Account created. The user can sign in with the supplied password.");
    setForm({ name: "", email: "", password: "", role: "STAFF" });
    await client.invalidateQueries({ queryKey: ["users"] });
  }});
  const errors = create.error?.response?.data ?? {};
  const change = (event) => setForm({ ...form, [event.target.name]: event.target.value });
  return <section className="space-y-6">
    <div><p className="eyebrow">Store management</p><h1 className="mt-2 text-3xl font-bold">User Management</h1><p className="mt-2 text-slate-500">Create staff accounts and review store users.</p></div>
    {message && <p role="status" className="rounded bg-green-50 p-3 text-green-700">{message}</p>}
    <form className="panel space-y-4 p-5" onSubmit={(event) => { event.preventDefault(); setMessage(""); create.mutate({ ...form, name: form.name.trim(), email: form.email.trim() }); }}>
      <h2 className="text-xl font-bold">Create account</h2>
      <div className="grid gap-4 sm:grid-cols-2">{[["name", "Name", "text"], ["email", "Email", "email"], ["password", "Initial password", "password"]].map(([name, label, type]) => <div key={name}><label className="field-label" htmlFor={`user-${name}`}>{label}</label><input id={`user-${name}`} className="field-input" name={name} type={type} value={form[name]} onChange={change} required minLength={name === "password" ? 8 : undefined} maxLength={name === "password" ? 72 : 255} autoComplete={name === "password" ? "new-password" : "off"} disabled={create.isPending} />{errors[name] && <p className="field-error">{errors[name]}</p>}</div>)}
        <div><label className="field-label" htmlFor="user-role">Role</label><select id="user-role" className="field-input" name="role" value={form.role} onChange={change} disabled={create.isPending}>{["CUSTOMER", "STAFF", "ADMIN"].map((role) => <option key={role}>{role}</option>)}</select></div>
      </div>
      {create.isError && <p role="alert" className="field-error">{errors.message || "Unable to create account. Check the fields and try again."}</p>}
      <button className="btn-primary" disabled={create.isPending}>{create.isPending ? "Creating…" : "Create account"}</button>
    </form>
    <div className="panel p-5"><label className="field-label" htmlFor="user-search">Search users</label><input id="user-search" className="field-input" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Name, email or role" /><button className="btn-outline mt-3" disabled={users.isFetching} onClick={() => users.refetch()}>Refresh</button></div>
    {users.isPending ? <p role="status">Loading users…</p> : users.isError ? <p role="alert" className="text-red-700">Unable to load users. <button className="underline" onClick={() => users.refetch()}>Retry</button></p> : <div className="panel overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{["ID", "Name", "Email", "Role"].map((label) => <th scope="col" className="p-4" key={label}>{label}</th>)}</tr></thead><tbody>{users.data.filter((user) => `${user.name} ${user.email} ${user.role}`.toLowerCase().includes(search.toLowerCase())).map((user) => <tr key={user.id} className="border-t border-slate-100"><td className="p-4">{user.id}</td><td className="p-4">{user.name}</td><td className="p-4">{user.email}</td><td className="p-4"><RoleEditor key={`${user.id}-${user.role}`} user={user} /></td></tr>)}</tbody></table>{!users.data.some((user) => `${user.name} ${user.email} ${user.role}`.toLowerCase().includes(search.toLowerCase())) && <p className="p-5">No users match this search.</p>}</div>}
  </section>;
}
