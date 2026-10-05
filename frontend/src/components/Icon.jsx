const paths = {
  phone: <><path d="M4 13v-2a8 8 0 0 1 16 0v2M4 13H2v5h4v-5H4Zm16 0h2v5h-4v-5h2ZM20 18v1a3 3 0 0 1-3 3h-4" /></>,
  search: <><circle cx="10" cy="10" r="6" /><path d="m15 15 6 6" /></>,
  arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
  cart: <><path d="M3 3h2l3 12h11l2-8H6" /><circle cx="9" cy="20" r="1" /><circle cx="18" cy="20" r="1" /></>,
  tools: <><path d="m14 6 4 4m-6 2-8 8-2-2 8-8" /><path d="M14 3a6 6 0 0 0-4 8l3 3a6 6 0 0 0 8-7l-4 4-4-4 4-4Z" /></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  bolt: <path d="m13 2-9 12h7l-1 8 10-13h-7l1-7Z" />,
  chip: <><rect x="6" y="6" width="12" height="12" rx="2" /><path d="M9 2v4m6-4v4M9 18v4m6-4v4M2 9h4m-4 6h4m12-6h4m-4 6h4M10 10h4v4h-4z" /></>,
  paint: <><rect x="3" y="3" width="14" height="6" rx="2" /><path d="M17 6h4v7h-9v3" /><rect x="10" y="16" width="4" height="6" rx="1" /></>,
  box: <><path d="m12 3 9 5v9l-9 5-9-5V8l9-5Zm-9 5 9 5 9-5m-9 5v9M7 5.8l10 5.5" /></>,
  filter: <><path d="M4 7h16M4 17h16" /><circle cx="9" cy="7" r="2" fill="currentColor" /><circle cx="15" cy="17" r="2" fill="currentColor" /></>,
};
export default function Icon({ name = "tools", className = "", ...props }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className={`icon ${className}`} {...props}>{paths[name] || paths.tools}</svg>;
}
