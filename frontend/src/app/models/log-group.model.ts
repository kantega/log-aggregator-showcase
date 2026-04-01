export interface LogGroup {
  id: number;
  name: string;
  status: 'OPEN' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

export interface LogEntry {
  id: number;
  content: string;
  createdAt: string;
}
