import { AfterViewInit, Component, OnDestroy } from '@angular/core';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  private observer?: IntersectionObserver;
  private scrollHandler?: () => void;
  private filterHandlers: Array<{ element: Element; handler: EventListener }> = [];

  ngAfterViewInit(): void {
    const navbar = document.getElementById('navbar');

    this.scrollHandler = () => {
      if (navbar) {
        navbar.classList.toggle('scrolled', window.scrollY > 40);
      }
    };

    window.addEventListener('scroll', this.scrollHandler);
    this.scrollHandler();

    const reveals = document.querySelectorAll('.reveal');

    this.observer = new IntersectionObserver((entries) => {
      entries.forEach((entry, index) => {
        if (entry.isIntersecting) {
          setTimeout(() => entry.target.classList.add('visible'), index * 80);
          this.observer?.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });

    reveals.forEach(el => this.observer?.observe(el));

    document.querySelectorAll('.filter-tag').forEach(tag => {
      const handler = () => {
        tag.classList.toggle('active');
        const icon = tag.querySelector('i');

        if (icon) {
          icon.className = tag.classList.contains('active')
            ? 'fas fa-check'
            : 'far fa-circle';
        }
      };

      tag.addEventListener('click', handler);
      this.filterHandlers.push({ element: tag, handler });
    });
  }

  ngOnDestroy(): void {
    if (this.scrollHandler) {
      window.removeEventListener('scroll', this.scrollHandler);
    }

    this.observer?.disconnect();

    this.filterHandlers.forEach(({ element, handler }) => {
      element.removeEventListener('click', handler);
    });

    this.filterHandlers = [];
  }
}