"use client";

import { useState } from "react";
import { BookOpen, Plus, Search, Edit, Trash2 } from "lucide-react";

// Mock dictionary data
const mockDictionary = [
    { id: 1, word: "Hello", description: "A greeting", category: "Greetings" },
    { id: 2, word: "Thanks", description: "Expression of gratitude", category: "Greetings" },
    { id: 3, word: "Yes", description: "Affirmative response", category: "Common" },
    { id: 4, word: "No", description: "Negative response", category: "Common" },
    { id: 5, word: "Please", description: "Polite request", category: "Common" },
];

export default function DictionaryPage() {
    const [searchTerm, setSearchTerm] = useState("");
    const [dictionary, setDictionary] = useState(mockDictionary);

    const filteredDictionary = dictionary.filter((item) =>
        item.word.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.description.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="space-y-6 md:space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-3xl md:text-4xl font-extrabold text-duo-text">Dictionary</h1>
                    <p className="text-lg font-semibold text-gray-600 mt-1">
                        Manage sign language entries
                    </p>
                </div>
                <button className="duo-button flex items-center gap-2 w-full sm:w-auto justify-center">
                    <Plus className="h-5 w-5" />
                    Add New Entry
                </button>
            </div>

            {/* Search Bar */}
            <div className="duo-card p-4">
                <div className="relative">
                    <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Search dictionary..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-12 pr-4 py-3 rounded-duo border-2 border-duo-border focus:outline-none focus:border-duo-green font-semibold text-duo-text bg-white"
                    />
                </div>
            </div>

            {/* Dictionary List */}
            <div className="grid gap-4">
                {filteredDictionary.length > 0 ? (
                    filteredDictionary.map((item) => (
                        <div key={item.id} className="duo-card p-6">
                            <div className="flex items-start justify-between gap-4">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-2">
                                        <div className="rounded-full p-2 bg-duo-green/20">
                                            <BookOpen className="h-5 w-5 text-duo-green" />
                                        </div>
                                        <h3 className="text-xl font-extrabold text-duo-text">
                                            {item.word}
                                        </h3>
                                        <span className="px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-600">
                                            {item.category}
                                        </span>
                                    </div>
                                    <p className="text-gray-600 font-semibold ml-11">
                                        {item.description}
                                    </p>
                                </div>
                                <div className="flex items-center gap-2">
                                    <button className="p-2 rounded-duo border-2 border-duo-border hover:bg-duo-green/10 hover:border-duo-green transition-all">
                                        <Edit className="h-5 w-5 text-duo-text" />
                                    </button>
                                    <button className="p-2 rounded-duo border-2 border-red-200 hover:bg-red-50 hover:border-red-300 transition-all">
                                        <Trash2 className="h-5 w-5 text-red-500" />
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="duo-card p-12 text-center">
                        <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                        <p className="text-gray-500 font-semibold text-lg">
                            No dictionary entries found
                        </p>
                        <p className="text-gray-400 text-sm mt-2">
                            {searchTerm ? "Try a different search term" : "Add your first entry to get started"}
                        </p>
                    </div>
                )}
            </div>

            {/* Stats */}
            <div className="grid gap-4 sm:grid-cols-3">
                <div className="duo-card p-4 text-center">
                    <p className="text-sm font-semibold text-gray-600 mb-1">Total Entries</p>
                    <p className="text-2xl font-extrabold text-duo-text">{dictionary.length}</p>
                </div>
                <div className="duo-card p-4 text-center">
                    <p className="text-sm font-semibold text-gray-600 mb-1">Categories</p>
                    <p className="text-2xl font-extrabold text-duo-text">
                        {new Set(dictionary.map((item) => item.category)).size}
                    </p>
                </div>
                <div className="duo-card p-4 text-center">
                    <p className="text-sm font-semibold text-gray-600 mb-1">Showing</p>
                    <p className="text-2xl font-extrabold text-duo-text">{filteredDictionary.length}</p>
                </div>
            </div>
        </div>
    );
}

