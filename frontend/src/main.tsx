import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { ApiError } from './api'
import { ErrorToasts, notifyError } from './feedback'
import './styles.css'

const queryClient=new QueryClient({mutationCache:new MutationCache({onError:notifyError}),queryCache:new QueryCache({onError:notifyError}),defaultOptions:{queries:{staleTime:30_000,retry:(count,error)=>count<1&&(!(error instanceof ApiError)||error.status===0||error.status>=500)}}})
createRoot(document.getElementById('root')!).render(<StrictMode><QueryClientProvider client={queryClient}><BrowserRouter><App/><ErrorToasts/></BrowserRouter></QueryClientProvider></StrictMode>)
