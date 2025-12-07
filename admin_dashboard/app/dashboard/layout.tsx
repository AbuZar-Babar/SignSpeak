"use client";

import { Sidebar } from '@/components/Sidebar'
import { useAuth } from '@/contexts/AuthContext'
import { useRouter } from 'next/navigation'
import { useEffect } from 'react'
import { LogOut } from 'lucide-react'

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode
}) {
    const { user, loading, logout } = useAuth()
    const router = useRouter()

    useEffect(() => {
        if (!loading && !user) {
            router.push('/login')
        }
    }, [user, loading, router])

    const handleLogout = () => {
        logout()
        router.push('/login')
    }

    if (loading) {
        return (
            <div className="flex h-screen items-center justify-center bg-duo-bg">
                <p className="text-duo-text font-semibold">Loading dashboard...</p>
            </div>
        )
    }

    // Allow access even without user for development (remove this in production)
    // if (!user) {
    //     return null // Will redirect in useEffect
    // }

    return (
        <div className="flex h-screen overflow-hidden bg-duo-bg">
            {/* Sidebar */}
            <aside className="hidden w-64 overflow-y-auto border-r-2 border-duo-border bg-white md:block">
                <Sidebar />
            </aside>

            {/* Main Content */}
            <main className="flex-1 overflow-y-auto">
                {/* Header with Logout */}
                <div className="sticky top-0 z-10 bg-white border-b-2 border-duo-border px-6 md:px-8 py-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            {user && (
                                <div>
                                    <p className="text-sm font-semibold text-gray-600">Logged in as</p>
                                    <p className="text-lg font-extrabold text-duo-text">{user.email || 'Admin'}</p>
                                </div>
                            )}
                        </div>
                        <button
                            onClick={handleLogout}
                            className="flex items-center gap-2 px-4 py-2 rounded-duo border-2 border-red-200 bg-red-50 text-red-600 font-bold hover:bg-red-100 hover:border-red-300 transition-all"
                        >
                            <LogOut className="h-5 w-5" />
                            <span className="hidden sm:inline">Logout</span>
                        </button>
                    </div>
                </div>
                
                <div className="container mx-auto p-6 md:p-8">
                    {children}
                </div>
            </main>
        </div>
    )
}
