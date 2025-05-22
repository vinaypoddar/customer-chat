import React, {useState} from 'react';
import axios from 'axios';

interface LoginProps {
    setUserType: (type: 'GUEST' | 'SUPPORT_AGENT' | null) => void;
}

const Login: React.FC<LoginProps> = ({setUserType}) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleLogin = async (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault();

        try {
            const response = await axios.post(
                'http://localhost:8080/api/auth/login',
                {},
                {
                    auth: {username, password},
                    withCredentials: true, // Ensure credentials are sent
                }
            );
            setUserType('SUPPORT_AGENT');
            setError('');
        } catch (err) {
            console.error('Login.tsx: Login error:', err);
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-expect-error
            if (err?.response?.status === 401) {
                setError('Invalid username or password');
                // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                // @ts-expect-error
            } else if (err?.response?.status === 403) {
                setError('Access forbidden. Please check your credentials.');
                // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                // @ts-expect-error
            } else if (err?.code === 'ERR_NETWORK') {
                setError('Network error. Is the server running at http://localhost:8080?');
            } else {
                setError('Login failed. Please try again later.');
            }
        }
    };

    return (
        <div className="w-full max-w-md bg-white rounded-lg shadow-lg p-6">
            <h1 className="text-2xl font-bold text-center mb-4">Agent Login</h1>
            {error && <p className="text-red-500 mb-4">{error}</p>}
            <input
                type="text"
                placeholder="Username"
                className="w-full p-2 mb-2 border rounded"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
            />
            <input
                type="password"
                placeholder="Password"
                className="w-full p-2 mb-2 border rounded"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            <button
                className="w-full bg-green-500 text-white p-2 rounded hover:bg-green-600"
                onClick={handleLogin}
            >
                Login
            </button>
            <button
                className="w-full mt-2 bg-gray-500 text-white p-2 rounded hover:bg-gray-600"
                onClick={() => setUserType(null)}
            >
                Back
            </button>
        </div>
    );
};

export default Login;