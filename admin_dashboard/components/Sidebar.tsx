import Link from 'next/link';

export default function Sidebar() {
    return (
        <div className="w-64 bg-gray-800 text-white min-h-screen p-4">
            <h2 className="text-2xl font-bold mb-8">SignSpeak Admin</h2>
            <nav className="space-y-2">
                <Link href="/dashboard" className="block px-4 py-2 rounded hover:bg-gray-700">
                    Dashboard
                </Link>
                <Link href="/dashboard/users" className="block px-4 py-2 rounded hover:bg-gray-700">
                    Users
                </Link>
                <Link href="/dashboard/complaints" className="block px-4 py-2 rounded hover:bg-gray-700">
                    Complaints
                </Link>
            </nav>
        </div>
    );
}
