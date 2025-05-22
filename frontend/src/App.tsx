import React, { useState, Component } from 'react';
import Chat from './components/Chat';
import Login from './components/Login';

interface ErrorBoundaryProps {
    children: React.ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
    error?: Error;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    state: ErrorBoundaryState = { hasError: false };

    static getDerivedStateFromError(error: Error) {
        return { hasError: true, error };
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="text-red-500 p-4">
                    <h1>Error: {this.state.error?.message}</h1>
                    <pre>{this.state.error?.stack}</pre>
                </div>
            );
        }
        return this.props.children;
    }
}

const App: React.FC = () => {
    const [userType, setUserType] = useState<'GUEST' | 'SUPPORT_AGENT' | 'AGENT' | null>(null);

    return (
        <ErrorBoundary>
            <div className="min-h-screen bg-gray-100 flex items-center justify-center">
                {!userType ? (
                    <div className="w-full max-w-md bg-white rounded-lg shadow-lg p-6">
                        <h1 className="text-2xl font-bold text-center mb-4">Customer Chat</h1>
                        <button
                            className="w-full mb-2 bg-blue-500 text-white p-2 rounded hover:bg-blue-600"
                            onClick={() => setUserType('GUEST')}
                        >
                            Join as Customer
                        </button>
                        <button
                            className="w-full bg-green-500 text-white p-2 rounded hover:bg-green-600"
                            onClick={() => setUserType('AGENT')}
                        >
                            Login as Agent
                        </button>
                    </div>
                ) : userType === 'AGENT' ? (
                    <Login setUserType={setUserType} />
                ) : (
                    <Chat userType={userType} setUserType={setUserType} />
                )}
            </div>
        </ErrorBoundary>
    );
};

export default App;