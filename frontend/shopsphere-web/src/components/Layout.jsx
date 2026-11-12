import { Outlet } from 'react-router-dom'
import Header from './Header.jsx'

export default function Layout() {
  return (
    <>
      <Header />
      <main className="container">
        <Outlet />
      </main>
      <footer className="footer">© {new Date().getFullYear()} ShopSphere</footer>
    </>
  )
}
