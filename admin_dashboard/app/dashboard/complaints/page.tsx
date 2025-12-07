"use client";
import { useEffect, useState } from 'react';

// Mock data
const MOCK_COMPLAINTS = [
    { id: '1', user: 'John Doe', title: 'App crashes on login', status: 'Pending', date: '2023-10-25' },
    { id: '2', user: 'Jane Smith', title: 'Translation inaccurate', status: 'Resolved', date: '2023-10-24' },
];

export default function ComplaintsPage() {
    const [complaints, setComplaints] = useState(MOCK_COMPLAINTS);

    useEffect(() => {
        // TODO: Fetch complaints from backend
    }, []);

    const handleStatusChange = (id: string, newStatus: string) => {
        // TODO: Update status in backend
        setComplaints(complaints.map(c => c.id === id ? { ...c, status: newStatus } : c));
    };

    return (
        <div>
            <h1 className="text-3xl font-bold mb-6">Complaints</h1>
            <div className="bg-white rounded shadow overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Title</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {complaints.map((complaint) => (
                            <tr key={complaint.id}>
                                <td className="px-6 py-4 whitespace-nowrap">{complaint.user}</td>
                                <td className="px-6 py-4 whitespace-nowrap">{complaint.title}</td>
                                <td className="px-6 py-4 whitespace-nowrap">{complaint.date}</td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                    ${complaint.status === 'Resolved' ? 'bg-green-100 text-green-800' :
                                            complaint.status === 'Pending' ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'}`}>
                                        {complaint.status}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {complaint.status !== 'Resolved' && (
                                        <button
                                            onClick={() => handleStatusChange(complaint.id, 'Resolved')}
                                            className="text-green-600 hover:text-green-900 mr-4"
                                        >
                                            Resolve
                                        </button>
                                    )}
                                    {complaint.status !== 'Rejected' && (
                                        <button
                                            onClick={() => handleStatusChange(complaint.id, 'Rejected')}
                                            className="text-red-600 hover:text-red-900"
                                        >
                                            Reject
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
