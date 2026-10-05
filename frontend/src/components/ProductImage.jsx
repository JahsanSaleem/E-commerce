import apiClient from "../services/apiClient.js";
import { useState } from "react";

export default function ProductImage({ product, className = "" }) {
  const [failedUrl, setFailedUrl] = useState(null);
  const stored = product.imageUrl;
  const url = stored?.startsWith("/api/media/") ? new URL(stored, apiClient.defaults.baseURL || window.location.origin).href : stored;
  const usable = url && /^(https?:\/\/|\/)/i.test(url) && failedUrl !== url;
  return usable ? (
    <img src={url} alt={product.name} onError={() => setFailedUrl(url)}
      className={`object-contain ${className}`} loading="lazy" />
  ) : (
    <div className={`flex items-center justify-center bg-slate-100 text-slate-400 ${className}`}>
      <span className="text-sm">No image available</span>
    </div>
  );
}
