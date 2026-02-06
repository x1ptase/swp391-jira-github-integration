import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import './index.css';
import './App.css';
import './pages/Login.css';



createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
