import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App shell', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('shows the product name and links to both sections', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.app-title')?.textContent).toContain('ACME Salary Management');
    const links = Array.from(element.querySelectorAll<HTMLAnchorElement>('nav a'));
    expect(links.map((link) => link.getAttribute('href'))).toEqual(['/employees', '/insights']);
  });
});
