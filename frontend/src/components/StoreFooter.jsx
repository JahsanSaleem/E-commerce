import { Link } from "react-router-dom";
import Icon from "./Icon.jsx";
import { storeContact } from "../config/storeContact.js";

export default function StoreFooter({ brand, categories = [], user }) {
  return <footer className="store-footer">
    <div className="site-width footer-top">
      <section className="footer-contact" aria-labelledby="footer-contact-title">
        {brand}
        <p className="contact-example">Example contact details</p>
        <div className="footer-phone"><Icon name="phone" /><div><p>Have a question? Contact our store</p><span>{storeContact.phone}</span></div></div>
        <h2 id="footer-contact-title">Contact info</h2>
        <address>{storeContact.address}<br /><span>{storeContact.email}</span></address>
      </section>
      <nav className="footer-links" aria-labelledby="footer-navigation-title">
        <h2 id="footer-navigation-title">Quick navigation</h2>
        <Link to="/">Home</Link><Link to="/products">Shop all products</Link><Link to="/cart">Shopping cart</Link>
      </nav>
      <nav className="footer-links" aria-labelledby="footer-account-title">
        <h2 id="footer-account-title">Customer care</h2>
        <Link to={user ? "/account" : "/login"}>My account</Link><Link to="/orders">Track your orders</Link>
        {!user && <Link to="/register">Create an account</Link>}
      </nav>
      <nav className="footer-links" aria-labelledby="footer-departments-title">
        <h2 id="footer-departments-title">Shop departments</h2>
        {categories.map(category => <Link key={category.categoryId} to={`/products?categoryId=${category.categoryId}`}>{category.name}</Link>)}
        {categories.length === 0 && <Link to="/products">Browse products</Link>}
      </nav>
    </div>
    <div className="footer-copyright"><div className="site-width"><span>© {new Date().getFullYear()} <strong>Mustafa Hardware</strong> — All rights reserved.</span><span>Tools, hardware & electronics</span></div></div>
  </footer>;
}
