import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { JobBoardService } from '../../services/job-board.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SkillStoreService } from '../../services/skill-store.service';
import {
  CareerInsightResponse,
  CareerSuggestionDto,
} from '../../models/job-board.models';
import {
  accordionExpand,
  fadeIn,
  slideInRight,
} from '../../animations/job-board.animations';

@Component({
  selector: 'app-career-insights',
  templateUrl: './career-insights.component.html',
  styleUrls: ['./career-insights.component.scss'],
  animations: [slideInRight, fadeIn, accordionExpand],
})
export class CareerInsightsComponent implements OnInit, OnDestroy {
  skills: string[] = [];
  newSkillInput = '';
  roadmap: any = null;
  suggestions: CareerSuggestionDto[] = [];
  totalIncomeBoost = 0;
  loading = false;
  error: string | null = null;
  generated = false;
  roadmapVisible = false;
  incomeBarActive = false;
  completionPercent = 0;
  hudVisible = false;
  private userId!: number;
  activeView: 'timeline' | '3d' = 'timeline';

  // 3D View State
  private threeLoaded = false;
  private scene: any;
  private camera: any;
  private renderer: any;
  private controls: any;
  private animationFrameId: number = 0;
  private nodes: any[] = [];
  private stars: any;
  private pathMesh: any;
  private pathCurve: any;
  private activeTorus: any;
  private clock: any;

  public activeNodeIndex = 0;
  public selectedNode: any = null; // Holds the currently viewed step
  public is3DPanelOpen = false;
  private raycaster: any;
  private mouse: any;
  private hoveredNode: any = null;
  private cameraTargetPos: any = null;
  private cameraTargetLook: any = null;

  constructor(
    private jobBoardService: JobBoardService,
    private authService: AuthService,
    private skillStore: SkillStoreService,
    private el: ElementRef,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    const uid = this.authService.getCurrentAuthUser()?.userId;
    this.userId = uid ?? 0;
    this.skills = this.skillStore.getSkills();
  }

  setView(view: 'timeline' | '3d'): void {
    this.activeView = view;
    if (view === '3d') {
      this.hudVisible = false;
      setTimeout(() => this.init3DScene(), 100);
    } else {
      this.destroy3DScene();
    }
  }

  private loadThreeJs(): Promise<void> {
    return new Promise((resolve) => {
      if ((window as any).THREE) {
        resolve();
        return;
      }
      const script = document.createElement('script');
      script.src =
        'https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js';
      script.onload = () => {
        const controlsScript = document.createElement('script');
        controlsScript.src =
          'https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js';
        controlsScript.onload = () => resolve();
        document.head.appendChild(controlsScript);
      };
      document.head.appendChild(script);
    });
  }

  private async init3DScene() {
    if (!this.roadmap || !this.roadmap.steps) return;
    await this.loadThreeJs();
    this.threeLoaded = true;

    const THREE = (window as any).THREE;
    const container = document.getElementById('three-canvas-container');
    if (!container) return;

    // Setup Scene
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color('#1a1a2e');
    this.scene.fog = new THREE.FogExp2(0x1a1a2e, 0.03);

    // Setup Camera
    const width = container.clientWidth;
    const height = container.clientHeight;
    this.camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    this.camera.position.set(0, 5, 10);

    // Setup Renderer
    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    container.innerHTML = '';
    container.appendChild(this.renderer.domElement);

    // Setup Controls
    if (THREE.OrbitControls) {
      this.controls = new THREE.OrbitControls(
        this.camera,
        this.renderer.domElement,
      );
      this.controls.enableDamping = true;
      this.controls.dampingFactor = 0.05;
      this.controls.autoRotate = true;
      this.controls.autoRotateSpeed = 0.5;
    }

    // Lights
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    this.scene.add(ambientLight);
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
    dirLight.position.set(10, 20, 10);
    this.scene.add(dirLight);

    // Stars
    const starGeo = new THREE.BufferGeometry();
    const starMat = new THREE.PointsMaterial({
      color: 0xffffff,
      size: 0.1,
      transparent: true,
      opacity: 0.8,
    });
    const starVerts = [];
    for (let i = 0; i < 2000; i++) {
      starVerts.push(
        (Math.random() - 0.5) * 100,
        (Math.random() - 0.5) * 100,
        (Math.random() - 0.5) * 100,
      );
    }
    starGeo.setAttribute(
      'position',
      new THREE.Float32BufferAttribute(starVerts, 3),
    );
    this.stars = new THREE.Points(starGeo, starMat);
    this.scene.add(this.stars);

    // Path & Nodes
    const points: any[] = [];
    this.nodes = [];
    const steps = this.roadmap.steps;

    steps.forEach((step: any, i: number) => {
      // Wind the path naturally
      const x = Math.sin(i * 1.5) * 4;
      const z = -i * 10;
      const y = Math.cos(i * 1.2) * 2;
      const pos = new THREE.Vector3(x, y, z);
      points.push(pos);

      // Node Sphere
      const isCompleted = i < this.activeNodeIndex;
      const isActive = i === this.activeNodeIndex;
      const color = isActive ? '#E8735A' : isCompleted ? '#22c55e' : '#6C63FF';
      const radius = isActive ? 1.2 : 0.8;

      const geo = new THREE.SphereGeometry(radius, 32, 32);
      const mat = new THREE.MeshStandardMaterial({
        color: color,
        emissive: color,
        emissiveIntensity: isActive ? 0.4 : isCompleted ? 0.2 : 0.1,
        transparent: true,
        opacity: isActive || isCompleted ? 1.0 : 0.6,
      });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.copy(pos);
      mesh.userData = { step, index: i, originalScale: 1.0, isHovered: false };
      this.scene.add(mesh);
      this.nodes.push(mesh);

      // Particle Burst on spawn (simulated via scale animation later)
    });

    // Active Torus
    const torusGeo = new THREE.TorusGeometry(1.8, 0.05, 16, 100);
    const torusMat = new THREE.MeshBasicMaterial({ color: 0xffd700 });
    this.activeTorus = new THREE.Mesh(torusGeo, torusMat);
    if (this.nodes.length > 0) {
      this.activeTorus.position.copy(this.nodes[this.activeNodeIndex].position);
      this.activeTorus.rotation.x = Math.PI / 2;
      this.scene.add(this.activeTorus);
    }

    // Path Tube
    this.pathCurve = new THREE.CatmullRomCurve3(points);
    const tubeGeo = new THREE.TubeGeometry(this.pathCurve, 100, 0.1, 8, false);
    const tubeMat = new THREE.MeshStandardMaterial({
      color: '#E8735A',
      emissive: '#E8735A',
      emissiveIntensity: 0.5,
    });
    this.pathMesh = new THREE.Mesh(tubeGeo, tubeMat);

    // Path drawing animation (scale via drawRange)
    tubeGeo.setDrawRange(0, 0);
    this.scene.add(this.pathMesh);

    let drawCount = 0;
    let pathFinished = false;
    const totalDrawCount = tubeGeo.index
      ? tubeGeo.index.count
      : tubeGeo.attributes.position.count;
    const drawSpeed = totalDrawCount / (2.5 * 60); // 2.5 seconds at 60fps

    // Interaction
    this.raycaster = new THREE.Raycaster();
    this.mouse = new THREE.Vector2();
    this.clock = new THREE.Clock();

    container.addEventListener(
      'mousemove',
      (e: MouseEvent) => this.onMouseMove(e, container),
      false,
    );
    container.addEventListener(
      'click',
      (e: MouseEvent) => this.onClick(e),
      false,
    );
    window.addEventListener('resize', this.onWindowResize, false);

    // Initial Camera Pos
    if (this.nodes.length > 0) {
      const firstPos = this.nodes[0].position;
      this.camera.position.set(firstPos.x, firstPos.y + 4, firstPos.z + 10);
      if (this.controls) this.controls.target.copy(firstPos);
    }

    // Animation Loop
    const animate = () => {
      this.animationFrameId = requestAnimationFrame(animate);
      const time = this.clock.getElapsedTime();

      // Draw Path
      if (drawCount < totalDrawCount) {
        drawCount += drawSpeed;
        tubeGeo.setDrawRange(0, Math.floor(drawCount));
      } else if (!pathFinished) {
        pathFinished = true;
        setTimeout(() => {
          this.hudVisible = true;
        }, 200);
      }

      // Stars animation
      if (this.stars) {
        this.stars.rotation.y = time * 0.02;
        this.stars.rotation.x = time * 0.01;
      }

      // Active Node pulsing & Torus rotation
      if (this.nodes[this.activeNodeIndex]) {
        const activeMesh = this.nodes[this.activeNodeIndex];
        const scale = 1.0 + Math.sin(time * 3) * 0.15;
        activeMesh.scale.set(scale, scale, scale);

        if (this.activeTorus) {
          this.activeTorus.position.copy(activeMesh.position);
          this.activeTorus.rotation.z = time * 1.5;
        }
      }

      // Hover lerp
      this.nodes.forEach((node, i) => {
        if (i !== this.activeNodeIndex) {
          const targetScale = node.userData.isHovered ? 1.2 : 1.0;
          node.scale.lerp(
            new THREE.Vector3(targetScale, targetScale, targetScale),
            0.1,
          );
        }
      });

      // Camera flight lerp
      if (this.cameraTargetPos && this.controls) {
        this.controls.autoRotate = false;
        this.camera.position.lerp(this.cameraTargetPos, 0.05);
        this.controls.target.lerp(this.cameraTargetLook, 0.05);

        if (this.camera.position.distanceTo(this.cameraTargetPos) < 0.1) {
          this.cameraTargetPos = null; // Arrived
        }
      }

      if (this.controls) this.controls.update();
      this.renderer.render(this.scene, this.camera);
    };

    animate();
  }

  private onMouseMove(event: MouseEvent, container: HTMLElement) {
    if (!this.threeLoaded || !this.raycaster) return;
    const rect = container.getBoundingClientRect();
    this.mouse.x =
      ((event.clientX - rect.left) / container.clientWidth) * 2 - 1;
    this.mouse.y =
      -((event.clientY - rect.top) / container.clientHeight) * 2 + 1;

    this.raycaster.setFromCamera(this.mouse, this.camera);
    const intersects = this.raycaster.intersectObjects(this.nodes);

    if (intersects.length > 0) {
      if (this.hoveredNode !== intersects[0].object) {
        if (this.hoveredNode) this.hoveredNode.userData.isHovered = false;
        this.hoveredNode = intersects[0].object;
        this.hoveredNode.userData.isHovered = true;
        container.style.cursor = 'pointer';
      }
    } else {
      if (this.hoveredNode) {
        this.hoveredNode.userData.isHovered = false;
        this.hoveredNode = null;
        container.style.cursor = 'default';
      }
    }
  }

  private onClick(event: MouseEvent) {
    if (!this.threeLoaded || !this.raycaster) return;
    this.raycaster.setFromCamera(this.mouse, this.camera);
    const intersects = this.raycaster.intersectObjects(this.nodes);

    if (intersects.length > 0) {
      const clickedNode = intersects[0].object;
      this.flyToNode(clickedNode.userData.index);
    } else {
      // Clicked outside
      this.close3DPanel();
    }
  }

  public flyToNode(index: number) {
    const THREE = (window as any).THREE;
    if (!this.nodes[index]) return;

    // Reset previous active node visually
    if (this.activeNodeIndex !== index && this.nodes[this.activeNodeIndex]) {
      const prev = this.nodes[this.activeNodeIndex];
      prev.scale.set(1, 1, 1);
      const isCompleted = this.activeNodeIndex < index;
      const color = isCompleted ? '#22c55e' : '#6C63FF';
      (prev.material as any).color.set(color);
      (prev.material as any).emissive.set(color);
      (prev.material as any).emissiveIntensity = isCompleted ? 0.2 : 0.1;
      (prev.material as any).opacity = 0.6;
    }

    this.activeNodeIndex = index;
    const targetNode = this.nodes[index];
    this.selectedNode = targetNode.userData.step;
    this.is3DPanelOpen = true;

    // Update active node visually
    (targetNode.material as any).color.set('#E8735A');
    (targetNode.material as any).emissive.set('#E8735A');
    (targetNode.material as any).emissiveIntensity = 0.4;
    (targetNode.material as any).opacity = 1.0;

    // Set camera targets
    const pos = targetNode.position;
    this.cameraTargetPos = new THREE.Vector3(pos.x + 3, pos.y + 2, pos.z + 8);
    this.cameraTargetLook = new THREE.Vector3(pos.x, pos.y, pos.z);
  }

  public close3DPanel() {
    this.is3DPanelOpen = false;
    if (this.controls) this.controls.autoRotate = true;
  }

  public nextNode() {
    if (this.activeNodeIndex < this.nodes.length - 1) {
      this.flyToNode(this.activeNodeIndex + 1);
    }
  }

  public prevNode() {
    if (this.activeNodeIndex > 0) {
      this.flyToNode(this.activeNodeIndex - 1);
    }
  }

  private onWindowResize = () => {
    if (!this.camera || !this.renderer) return;
    const container = document.getElementById('three-canvas-container');
    if (!container) return;
    const width = container.clientWidth;
    const height = container.clientHeight;
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);
  };

  private destroy3DScene() {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    window.removeEventListener('resize', this.onWindowResize);
    const container = document.getElementById('three-canvas-container');
    if (container) {
      container.innerHTML = '';
    }
    this.threeLoaded = false;
  }

  trackBySkill(index: number, skill: string): string {
    return skill;
  }

  addSkill(event: Event): void {
    event.preventDefault();
    const skill = this.newSkillInput.trim();
    if (skill && !this.skills.includes(skill)) {
      this.skills = this.skillStore.addSkill(skill);
    }
    this.newSkillInput = '';
  }

  removeSkill(skill: string): void {
    this.skills = this.skillStore.removeSkill(skill);
  }

  hasSkill(skill: string): boolean {
    const normalized = String(skill ?? '')
      .trim()
      .toLowerCase();
    return (
      normalized.length > 0 &&
      this.skills.some((s) => s.trim().toLowerCase() === normalized)
    );
  }

  private parseRoadmapFromGeminiResponse(response: any): any {
    if (response?.choices) {
      let text = response.choices[0]?.message?.content ?? '';
      text = text
        .replace(/```json/g, '')
        .replace(/```/g, '')
        .trim();
      const parsed = JSON.parse(text);
      if (
        !parsed?.steps ||
        !Array.isArray(parsed.steps) ||
        parsed.steps.length === 0
      ) {
        throw new Error('Invalid structure');
      }
      parsed.steps = parsed.steps.map((step: any, i: number) => ({
        id: step?.id ?? i + 1,
        title: step?.title ?? 'Step ' + (i + 1),
        description: step?.description ?? '',
        difficultyLevel: step?.difficultyLevel ?? 'Intermediate',
        estimatedWeeks: step?.estimatedWeeks ?? 4,
        hoursPerDay: step?.hoursPerDay ?? 2,
        incomeBoostThisStep: step?.incomeBoostThisStep ?? 0,
        microCurriculum: Array.isArray(step?.microCurriculum)
          ? step.microCurriculum
          : [],
        resources: Array.isArray(step?.resources) ? step.resources : [],
        portfolioProject: step?.portfolioProject ?? '',
        prerequisiteSkills: Array.isArray(step?.prerequisiteSkills)
          ? step.prerequisiteSkills
          : [],
        skillsUnlocked: Array.isArray(step?.skillsUnlocked)
          ? step.skillsUnlocked
          : [],
        demandLevel: step?.demandLevel ?? 'High',
        color: step?.color ?? '#E8735A',
      }));
      return parsed;
    }

    const parts = response?.candidates?.[0]?.content?.parts;
    let text = Array.isArray(parts)
      ? parts
          .map((p: any) => p?.text)
          .filter(
            (v: unknown) =>
              typeof v === 'string' && String(v).trim().length > 0,
          )
          .join('\n')
      : response?.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!text) {
      const blockReason = response?.promptFeedback?.blockReason;
      if (blockReason) {
        throw new Error(`Gemini blocked this prompt: ${blockReason}`);
      }
      throw new Error('No text in Gemini response');
    }

    text = String(text)
      .replace(/```(?:json)?/gi, '')
      .replace(/```/g, '')
      .trim();
    const parsed = JSON.parse(text);
    if (
      !parsed?.steps ||
      !Array.isArray(parsed.steps) ||
      parsed.steps.length === 0
    ) {
      throw new Error('Invalid structure');
    }

    parsed.steps = parsed.steps.map((step: any, i: number) => ({
      id: step?.id ?? i + 1,
      title: step?.title ?? 'Step ' + (i + 1),
      description: step?.description ?? '',
      difficultyLevel: step?.difficultyLevel ?? 'Intermediate',
      estimatedWeeks: step?.estimatedWeeks ?? 4,
      hoursPerDay: step?.hoursPerDay ?? 2,
      incomeBoostThisStep: step?.incomeBoostThisStep ?? 0,
      microCurriculum: Array.isArray(step?.microCurriculum)
        ? step.microCurriculum
        : [],
      resources: Array.isArray(step?.resources) ? step.resources : [],
      portfolioProject: step?.portfolioProject ?? '',
      prerequisiteSkills: Array.isArray(step?.prerequisiteSkills)
        ? step.prerequisiteSkills
        : [],
      skillsUnlocked: Array.isArray(step?.skillsUnlocked)
        ? step.skillsUnlocked
        : [],
      demandLevel: step?.demandLevel ?? 'High',
      color: step?.color ?? '#E8735A',
    }));

    return parsed;
  }

  generateRoadmap(): void {
    if (this.skills.length === 0) {
      this.error = 'Please add at least one skill first';
      return;
    }
    this.loading = true;
    this.error = null;
    this.generated = false;
    this.roadmapVisible = false;

    const apiKey = '';
    const url = 'https://api.groq.com/openai/v1/chat/completions';
    let retryCount = 0;

    const prompt = `You are a senior tech career coach and industry expert with deep knowledge of the 2025 job market. Generate a highly detailed and realistic career roadmap for a freelancer with these current skills: ${this.skills.join(', ')}.

Return ONLY a raw JSON object. No markdown. No backticks. No explanation. No text before or after the JSON. Just the JSON object.

{
  "targetRole": "specific senior role title based on the skills provided",
  "currentLevel": "honest assessment of their current level",
  "totalWeeks": number between 16 and 32,
  "totalIncomeBoost": number between 30 and 60,
  "currentRate": realistic hourly rate for their current level as a number,
  "projectedRate": realistic projected hourly rate as a number,
  "difficulty": "Easy or Moderate or Hard",
  "steps": [
    {
      "id": 1,
      "title": "specific skill or technology to master",
      "description": "2 to 3 sentences explaining exactly why this skill is critical in 2025, what companies use it, and what doors it opens for the freelancer",
      "difficultyLevel": "Beginner or Intermediate or Advanced or Expert",
      "estimatedWeeks": number,
      "hoursPerDay": number between 1 and 3,
      "incomeBoostThisStep": number between 5 and 15,
      "microCurriculum": [
        { "week": 1, "focus": "very specific topic with real subtopics covered this week" },
        { "week": 2, "focus": "very specific topic with real subtopics covered this week" },
        { "week": 3, "focus": "very specific topic with real subtopics covered this week" },
        { "week": 4, "focus": "very specific topic with real subtopics covered this week" }
      ],
      "resources": [
        "Full name of real existing course or documentation with platform name - for example: TypeScript Deep Dive by Basarat Ali Syed on GitBook",
        "Full name of real existing course or documentation with platform name",
        "Full name of real existing course or documentation with platform name"
      ],
      "portfolioProject": "Very specific impressive project that demonstrates mastery - not a todo app, not a simple CRUD. Something with real complexity like real-time features, authentication, third party integrations, performance optimization, or production-grade architecture",
      "prerequisiteSkills": ["skill1", "skill2"],
      "skillsUnlocked": ["skill1", "skill2", "skill3", "skill4"],
      "demandLevel": "Very High or High or Medium or Growing Fast",
      "color": "#E8735A"
    }
  ]
}

Generate exactly 4 or 5 steps. Every single field must be filled with real specific content. The microCurriculum must reflect what someone actually learns week by week. The resources must be real courses that exist right now. The portfolioProject must be something that would genuinely impress a technical recruiter on GitHub.`;

    const body = {
      model: 'llama-3.3-70b-versatile',
      messages: [{ role: 'user', content: prompt }],
      temperature: 0.7,
    };

    const doRequest = (reqBody: any) => {
      this.http
        .post(url, reqBody, { headers: { Authorization: `Bearer ${apiKey}` } })
        .subscribe({
          next: (response: any) => {
            try {
              console.log('RAW GEMINI RESPONSE:', response);
              const parsed = this.parseRoadmapFromGeminiResponse(response);
              console.log('PARSED ROADMAP:', parsed);
              this.roadmap = parsed;
              this.loading = false;
              this.generated = true;
              this.incomeBarActive = false;
              setTimeout(() => {
                this.roadmapVisible = true;
                setTimeout(() => {
                  this.incomeBarActive = true;
                }, 600);
                if (this.activeView === '3d') {
                  this.destroy3DScene();
                  this.init3DScene();
                }
              }, 100);
            } catch (e: any) {
              this.loading = false;
              this.error =
                e?.message ?? 'Unable to parse AI response. Please try again.';
              console.error('Parse error:', e);
            }
          },
          error: (err: any) => {
            const status = err?.status;
            if (status === 429 && retryCount < 2) {
              retryCount++;
              this.error = 'AI quota exceeded. Retrying in 15 seconds...';
              setTimeout(() => {
                this.error = null;
                doRequest(reqBody);
              }, 15000);
              return;
            }
            if (status === 401) {
              this.loading = false;
              this.error = 'API key invalid';
              return;
            }
            this.loading = false;
            const apiMessage =
              err?.error?.error?.message ??
              err?.error?.message ??
              err?.message ??
              'Unable to generate career roadmap. Please try again.';
            this.error = apiMessage;
            console.error('Gemini API error:', err);
          },
        });
    };

    doRequest(body);
  }

  trendBadgeClass(trend: string | undefined): string {
    const map: Record<string, string> = {
      RISING: 'status-featured',
      DECLINING: 'status-hot',
      STABLE: 'status-new',
    };
    return map[String(trend ?? '')] ?? 'status-new';
  }

  trendKey(trend: unknown): string {
    return String(trend ?? 'STABLE');
  }

  trendLabel(trend: string | undefined): string {
    const map: Record<string, string> = {
      RISING: '↑ Rising',
      DECLINING: '↓ Declining',
      STABLE: '→ Stable',
    };
    return map[String(trend ?? '')] ?? String(trend ?? 'STABLE');
  }

  nodeInitials(skill: string): string {
    return (skill ?? '').substring(0, 2).toUpperCase();
  }

  coOccurrenceExplanation(s: CareerSuggestionDto): string {
    const pct = Math.round(((s.coOccurrenceRate ?? 0) as number) * 100);
    if (pct === 0) {
      return 'Trending skill — highly recommended to add to your profile';
    }
    return `${pct}% of jobs requiring your skills also need this`;
  }

  extractUrl(resource: string): string {
    const urlMatch = resource.match(/https?:\/\/[^\s,)]+/);
    if (urlMatch) return urlMatch[0];
    // Try to find domain patterns like gitbook.com, udemy.com etc
    const domainMatch = resource.match(
      /[\w-]+\.(com|io|org|dev|net|edu)(?:\/[\w\-\/]*)?/,
    );
    if (domainMatch) return 'https://' + domainMatch[0];
    // Fallback: Google search for the resource name
    return 'https://www.google.com/search?q=' + encodeURIComponent(resource);
  }

  ngOnDestroy(): void {
    this.destroy3DScene();
  }
}
