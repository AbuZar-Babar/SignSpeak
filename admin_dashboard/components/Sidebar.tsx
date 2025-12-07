"use client"

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { LayoutDashboard, Users, BookOpen, MessageSquare, Settings, LogOut } from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'

const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Users', href: '/dashboard/users', icon: Users },
    { name: 'Dictionary', href: '/dashboard/dictionary', icon: BookOpen },
    { name: 'Complaints', href: '/dashboard/complaints', icon: MessageSquare },
    { name: 'Settings', href: '/dashboard/settings', icon: Settings },
]

export function Sidebar() {
    const pathname = usePathname()
    const router = useRouter()
    const { logout } = useAuth()

    const handleLogout = () => {
        logout()
        router.push('/login')
    }

    return (
        <div className="flex h-full w-64 flex-col bg-white border-r-2 border-duo-border">
            <div className="flex h-16 items-center px-6 border-b-2 border-duo-border">
                <h1 className="text-xl font-bold text-duo-green">SignSpeak Admin</h1>
            </div>
            <nav className="flex-1 space-y-2 px-3 py-4">
                {navigation.map((item) => {
                    const isActive = pathname === item.href
                    return (
                        <Link
                            key={item.name}
                            href={item.href}
                            className={`flex items-center gap-3 rounded-duo px-3 py-2.5 text-sm font-bold transition-all ${
                                isActive
                                    ? 'bg-duo-green text-white shadow-duo-sm'
                                    : 'text-duo-text hover:bg-duo-green/10'
                            }`}
                        >
                            <item.icon className="h-5 w-5" />
                            {item.name}
                        </Link>
                    )
                })}
            </nav>
            <div className="border-t-2 border-duo-border p-4">
                <button 
                    onClick={handleLogout}
                    className="flex w-full items-center gap-3 rounded-duo px-3 py-2.5 text-sm font-bold text-duo-text transition-all hover:bg-red-50 hover:text-red-500"
                >
                    <LogOut className="h-5 w-5" />
                    Logout
                </button>
            </div>
        </div>
    )
}
