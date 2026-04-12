import { Component } from '@angular/core';

interface ChatUser {
  id: string;
  name: string;
  role: string;
  avatar: string;
  online: boolean;
  lastMessage: string;
  time: string;
  unreadCount?: number;
}

interface ChatMessage {
  sender: 'me' | 'other';
  content: string;
  time: string;
}

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent {

  selectedUserId = 'user-1';
  messageText = '';

  users: ChatUser[] = [
    {
      id: 'user-1',
      name: 'Sarah Johnson',
      role: 'Product Manager',
      avatar: 'https://i.pravatar.cc/150?img=1',
      online: true,
      lastMessage: 'Let’s finalize the contract today.',
      time: '2 min',
      unreadCount: 2
    },
    {
      id: 'user-2',
      name: 'Ahmed Ben Ali',
      role: 'Client',
      avatar: 'https://i.pravatar.cc/150?img=2',
      online: false,
      lastMessage: 'Waiting for your proposal.',
      time: '10 min'
    },
    {
      id: 'user-3',
      name: 'Emily Carter',
      role: 'UX Designer',
      avatar: 'https://i.pravatar.cc/150?img=3',
      online: true,
      lastMessage: 'Great work!',
      time: '1h'
    }
  ];

  messages: ChatMessage[] = [
    {
      sender: 'other',
      content: 'Hi, are you available for a quick update?',
      time: '10:00'
    },
    {
      sender: 'me',
      content: 'Yes of course, tell me.',
      time: '10:01'
    },
    {
      sender: 'other',
      content: 'We want to move forward with you.',
      time: '10:02'
    }
  ];

  get selectedUser(): ChatUser {
    return this.users.find(u => u.id === this.selectedUserId)!;
  }

  selectUser(id: string): void {
    this.selectedUserId = id;
  }

  sendMessage(): void {
    if (!this.messageText.trim()) return;

    this.messages.push({
      sender: 'me',
      content: this.messageText,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });

    this.messageText = '';
  }
}