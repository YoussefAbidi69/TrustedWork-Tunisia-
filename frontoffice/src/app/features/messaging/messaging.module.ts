import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MessagingRoutingModule } from './messaging-routing.module';
import { ChatComponent } from './chat/chat.component';


@NgModule({
  declarations: [
    ChatComponent
  ],
  imports: [
    CommonModule,
    MessagingRoutingModule,
    FormsModule
  ]
})
export class MessagingModule { }
