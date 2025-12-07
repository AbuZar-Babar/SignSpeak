import { Users, BookOpen, MessageSquare, TrendingUp, Camera, Settings, Edit } from 'lucide-react'
import Link from 'next/link'

// Mock Data (Replace with Firebase calls later)
const stats = [
    {
        name: 'Total Users',
        value: '2,543',
        change: '+12.5%',
        trend: 'up',
        icon: Users,
        color: '#2B70C9',
    },
    {
        name: 'Total Signs',
        value: '1,234',
        change: '+3.2%',
        trend: 'up',
        icon: BookOpen,
        color: '#58CC02',
    },
    {
        name: 'Active Complaints',
        value: '12',
        change: '-2.4%',
        trend: 'down',
        icon: MessageSquare,
        color: '#FF9600',
    },
    {
        name: 'Daily Translations',
        value: '45.2k',
        change: '+5.4%',
        trend: 'up',
        icon: TrendingUp,
        color: '#A855F7',
    },
]

const actionCards = [
    {
        title: 'Manage Users',
        subtitle: 'View and manage user accounts',
        icon: Users,
        color: '#2B70C9',
        href: '/dashboard/users',
    },
    {
        title: 'Update Dictionary',
        subtitle: 'Add, edit, or remove sign language entries',
        icon: Edit,
        color: '#58CC02',
        href: '/dashboard/dictionary',
    },
    {
        title: 'View Complaints',
        subtitle: 'Review and respond to user complaints',
        icon: MessageSquare,
        color: '#FF9600',
        href: '/dashboard/complaints',
    },
    {
        title: 'Settings',
        subtitle: 'Configure system settings',
        icon: Settings,
        color: '#A855F7',
        href: '/dashboard/settings',
    },
]

export default function DashboardPage() {
    return (
        <div className="space-y-6 md:space-y-8">
            {/* Welcome Header */}
            <div>
                <p className="text-lg font-semibold text-gray-600 mb-1">Welcome Back,</p>
                <h1 className="text-3xl md:text-4xl font-extrabold text-duo-text">Admin</h1>
            </div>

            {/* Stats Grid */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                {stats.map((stat) => (
                    <div
                        key={stat.name}
                        className="duo-card p-6"
                    >
                        <div className="flex items-center gap-4">
                            <div 
                                className="rounded-full p-3"
                                style={{ backgroundColor: `${stat.color}20` }}
                            >
                                <stat.icon 
                                    className="h-6 w-6" 
                                    style={{ color: stat.color }}
                                />
                            </div>
                            <div className="flex-1">
                                <p className="text-sm font-semibold text-gray-600">{stat.name}</p>
                                <h3 className="text-2xl font-extrabold text-duo-text">{stat.value}</h3>
                            </div>
                        </div>
                        <div className="mt-4 flex items-center gap-2">
                            <span
                                className={`text-xs font-bold ${
                                    stat.trend === 'up' ? 'text-duo-green' : 'text-red-500'
                                }`}
                            >
                                {stat.change}
                            </span>
                            <span className="text-xs text-gray-500 font-semibold">from last month</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* Action Cards */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {actionCards.map((card) => (
                    <Link key={card.title} href={card.href}>
                        <div 
                            className="duo-card p-6 h-32 cursor-pointer"
                            style={{ borderColor: card.color }}
                        >
                            <div className="flex items-center justify-between h-full">
                                <div className="flex-1">
                                    <h3 className="text-lg font-extrabold text-duo-text mb-2">
                                        {card.title}
                                    </h3>
                                    <p className="text-sm font-semibold text-gray-500">
                                        {card.subtitle}
                                    </p>
                                </div>
                                <div 
                                    className="rounded-full p-3"
                                    style={{ backgroundColor: `${card.color}20` }}
                                >
                                    <card.icon 
                                        className="h-7 w-7" 
                                        style={{ color: card.color }}
                                    />
                                </div>
                            </div>
                        </div>
                    </Link>
                ))}
            </div>

            {/* Recent Activity */}
            <div className="duo-card p-6">
                <h3 className="mb-4 text-lg font-extrabold text-duo-text">Recent Activity</h3>
                <div className="flex h-32 items-center justify-center rounded-duo border-2 border-dashed border-duo-border bg-duo-bg">
                    <p className="text-gray-500 font-semibold">Activity Feed Placeholder</p>
                </div>
            </div>
        </div>
    )
}
