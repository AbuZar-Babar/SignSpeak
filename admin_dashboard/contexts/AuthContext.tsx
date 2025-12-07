"use client";
import { createContext, useContext, useState, useEffect } from "react";

// Mock user type (replace with Firebase User type when Firebase is implemented)
interface MockUser {
    uid: string;
    email: string | null;
    displayName: string | null;
}

interface AuthContextType {
    user: MockUser | null;
    loading: boolean;
    login: (email: string, password: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType>({ 
    user: null, 
    loading: true,
    login: async () => {},
    logout: () => {}
});

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<MockUser | null>(null);
    const [loading, setLoading] = useState(true);

    // Check for stored session on mount
    useEffect(() => {
        const storedUser = localStorage.getItem('mock_user');
        if (storedUser) {
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    }, []);

    const login = async (email: string, password: string) => {
        // Mock login - accept any email/password for now
        // In production, this would call Firebase Auth
        const mockUser: MockUser = {
            uid: 'mock-user-123',
            email: email,
            displayName: email.split('@')[0],
        };
        setUser(mockUser);
        localStorage.setItem('mock_user', JSON.stringify(mockUser));
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem('mock_user');
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
