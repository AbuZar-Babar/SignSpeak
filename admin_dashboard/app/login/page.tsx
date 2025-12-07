"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../../contexts/AuthContext";
import { useEffect } from "react";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const router = useRouter();
    const { user, loading, login } = useAuth();

    useEffect(() => {
        if (!loading && user) {
            router.push("/dashboard");
        }
    }, [user, loading, router]);

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");
        try {
            if (!email || !password) {
                setError("Please enter both email and password");
                return;
            }
            await login(email, password);
            router.push("/dashboard");
        } catch (err: any) {
            setError("Failed to login: " + (err.message || "Unknown error"));
        }
    };

    return (
        <div className="flex min-h-screen items-center justify-center bg-duo-bg p-4">
            <div className="w-full max-w-md">
                <div className="duo-card p-8 space-y-6">
                    <div className="text-center space-y-2">
                        <h1 className="text-3xl font-extrabold text-duo-green">SignSpeak</h1>
                        <h2 className="text-xl font-bold text-duo-text">Admin Login</h2>
                    </div>
                    {error && (
                        <div className="p-3 rounded-duo bg-red-50 border-2 border-red-200">
                            <p className="text-red-600 text-center font-semibold text-sm">{error}</p>
                        </div>
                    )}
                    <form onSubmit={handleLogin} className="w-full space-y-5">
                        <div className="w-full space-y-2">
                            <label htmlFor="email" className="block text-sm font-bold text-duo-text w-full">
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                className="w-full px-4 py-3 rounded-duo border-2 border-duo-border focus:outline-none focus:border-duo-green font-semibold text-duo-text bg-white"
                                placeholder="admin@signspeak.com"
                            />
                        </div>
                        <div className="w-full space-y-2">
                            <label htmlFor="password" className="block text-sm font-bold text-duo-text w-full">
                                Password
                            </label>
                            <input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                className="w-full px-4 py-3 rounded-duo border-2 border-duo-border focus:outline-none focus:border-duo-green font-semibold text-duo-text bg-white"
                                placeholder="Enter your password"
                            />
                        </div>
                        <div className="w-full pt-2">
                            <button
                                type="submit"
                                className="w-full duo-button"
                            >
                                Login
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
