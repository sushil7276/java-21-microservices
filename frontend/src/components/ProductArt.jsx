import { useEffect, useState } from 'react';
import { Smartphone, Laptop, Headphones, Watch, Tablet, Package } from 'lucide-react';
import { categoryMeta } from '../utils/format';

const ICONS = { Smartphone, Laptop, Headphones, Watch, Tablet, Package };

/**
 * Product picture. Shows the photo from `imageUrl` when the product has one and it loads;
 * otherwise (no link, or the link is broken) a branded gradient tile with the category icon.
 */
export default function ProductArt({ category, imageUrl, alt = '', className = '', iconSize = 64 }) {
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);

  // a different product / edited link gets a fresh attempt
  useEffect(() => { setFailed(false); setLoaded(false); }, [imageUrl]);

  if (imageUrl && !failed) {
    return (
      <div className={`relative overflow-hidden bg-white ${className}`}>
        {!loaded && <div className="skeleton absolute inset-0 rounded-none" />}
        <img
          src={imageUrl}
          alt={alt}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"   /* many sites block hot-linking when a Referer is sent */
          onLoad={() => setLoaded(true)}
          onError={() => setFailed(true)}
          ref={(el) => { if (el && !loaded && el.complete && el.naturalWidth > 0) setLoaded(true); }}
          className={`h-full w-full object-contain p-3 transition duration-500 group-hover:scale-105 ${loaded ? 'opacity-100' : 'opacity-0'}`}
        />
      </div>
    );
  }

  const { gradient, icon } = categoryMeta(category);
  const Icon = ICONS[icon] ?? Package;
  return (
    <div className={`relative flex items-center justify-center overflow-hidden bg-gradient-to-br ${gradient} ${className}`}>
      <div className="absolute -left-8 -top-8 h-32 w-32 rounded-full bg-white/15 blur-sm" />
      <div className="absolute -bottom-10 -right-6 h-40 w-40 rounded-full bg-black/10" />
      <Icon size={iconSize} strokeWidth={1.4} className="relative text-white drop-shadow-lg transition-transform duration-500 group-hover:scale-110" />
    </div>
  );
}
