import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Users, MessageSquare, Briefcase, Heart, Search, Share2, Info, ChevronRight, Filter, UserPlus } from 'lucide-react';
import logo from './longhorn-logo.png';

// NOTE: Ensure LabServer.java is running on port 8080.

// --- 3. CUSTOM CANVAS GRAPH COMPONENT ---
const SimpleGraph = ({ nodes, links, width, height, onNodeClick, selectedNode, roommatePairs, referralPath }) => {
  const canvasRef = useRef(null);
  const animationRef = useRef(null);
  
  // Transform State (Pan x/y, Zoom k)
  const transform = useRef({ x: 0, y: 0, k: 1 });
  const interaction = useRef({ isDragging: false, startX: 0, startY: 0, hasMoved: false });

  const simulationNodes = useMemo(() => {
    return nodes.map(n => ({
      ...n,
      x: Math.random() * width,
      y: Math.random() * height,
      vx: 0, vy: 0
    }));
  }, [nodes]); 

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    
    const runSimulation = () => {
      const kForce = 150; 
      const centerPush = 0.05;
      
      // Physics - Repulsion
      for (let i = 0; i < simulationNodes.length; i++) {
        for (let j = i + 1; j < simulationNodes.length; j++) {
          const a = simulationNodes[i]; const b = simulationNodes[j];
          const dx = a.x - b.x; const dy = a.y - b.y;
          const dist = Math.sqrt(dx*dx + dy*dy) || 1;
          const force = (kForce * kForce) / dist;
          const fx = (dx / dist) * force; const fy = (dy / dist) * force;
          a.vx += fx / 50; a.vy += fy / 50;
          b.vx -= fx / 50; b.vy -= fy / 50;
        }
      }

      // Physics - Attraction
      links.forEach(link => {
        const s = simulationNodes.find(n => n.name === link.source);
        const t = simulationNodes.find(n => n.name === link.target);
        if (s && t) {
          const dx = t.x - s.x; const dy = t.y - s.y;
          const dist = Math.sqrt(dx*dx + dy*dy) || 1;
          const force = (dist * dist) / kForce;
          const fx = (dx / dist) * force; const fy = (dy / dist) * force;
          s.vx += fx / 100; s.vy += fy / 100;
          t.vx -= fx / 100; t.vy -= fy / 100;
        }
      });

      // Physics - Center Gravity
      simulationNodes.forEach(n => {
        n.vx += (width/2 - n.x) * centerPush;
        n.vy += (height/2 - n.y) * centerPush;
        n.vx *= 0.85; n.vy *= 0.85;
        n.x += n.vx; n.y += n.vy;
      });

      // --- RENDER START ---
      ctx.save();
      ctx.clearRect(0, 0, width, height);
      
      // Apply Transform
      const { x: tx, y: ty, k: tk } = transform.current;
      ctx.translate(tx, ty);
      ctx.scale(tk, tk);

      links.forEach(link => {
        const s = simulationNodes.find(n => n.name === link.source);
        const t = simulationNodes.find(n => n.name === link.target);
        if (s && t) {
          let strokeStyle = 'rgba(255, 255, 255, 0.2)';
          let lineWidth = 1;

          if (roommatePairs[s.name] === t.name) {
            strokeStyle = '#10B981'; 
            lineWidth = 4;
          } 
          else if (referralPath.length > 1) {
             for (let i = 0; i < referralPath.length - 1; i++) {
               if ((referralPath[i] === s.name && referralPath[i+1] === t.name) || 
                   (referralPath[i] === t.name && referralPath[i+1] === s.name)) {
                 strokeStyle = '#F59E0B'; 
                 lineWidth = 4;
               }
             }
          }

          ctx.beginPath();
          ctx.moveTo(s.x, s.y);
          ctx.lineTo(t.x, t.y);
          ctx.strokeStyle = strokeStyle;
          ctx.lineWidth = lineWidth;
          ctx.stroke();

          // Draw Weight Badge (Only if no roommates assigned)
          if (Object.keys(roommatePairs).length === 0) {
              const midX = (s.x + t.x) / 2;
              const midY = (s.y + t.y) / 2;
              
              ctx.beginPath();
              ctx.arc(midX, midY, 9/tk, 0, 2 * Math.PI); 
              ctx.fillStyle = '#1e293b'; 
              ctx.fill();
              
              ctx.fillStyle = '#cbd5e1'; 
              ctx.font = `bold ${10/tk}px sans-serif`; 
              ctx.textAlign = 'center';
              ctx.textBaseline = 'middle';
              ctx.fillText(link.weight, midX, midY);
          }
        }
      });

      simulationNodes.forEach(n => {
        const isSelected = selectedNode && selectedNode.name === n.name;
        ctx.beginPath();
        ctx.arc(n.x, n.y, isSelected ? 25 : 20, 0, 2 * Math.PI);
        ctx.fillStyle = isSelected ? '#bf5700' : '#3b82f6';
        ctx.fill();
        ctx.strokeStyle = '#1e293b';
        ctx.lineWidth = 3/tk;
        ctx.stroke();
        
        ctx.font = isSelected ? `bold ${14/tk}px sans-serif` : `${12/tk}px sans-serif`;
        ctx.fillStyle = '#fff';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(n.name, n.x, n.y);
      });

      ctx.restore(); 
      animationRef.current = requestAnimationFrame(runSimulation);
    };
    runSimulation();
    return () => cancelAnimationFrame(animationRef.current);
  }, [simulationNodes, links, width, height, selectedNode, roommatePairs, referralPath]);

  // --- INTERACTION HANDLERS ---
  const handleWheel = (e) => {
    e.preventDefault();
    const zoomIntensity = 0.1;
    const wheel = e.deltaY < 0 ? 1 : -1;
    const zoom = Math.exp(wheel * zoomIntensity);
    const rect = canvasRef.current.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    const { x, y, k } = transform.current;
    
    let newK = k * zoom;
    newK = Math.max(0.1, Math.min(newK, 5));

    const newX = mouseX - (mouseX - x) * (newK / k);
    const newY = mouseY - (mouseY - y) * (newK / k);

    transform.current = { x: newX, y: newY, k: newK };
  };

  const handleMouseDown = (e) => {
    interaction.current.isDragging = true;
    interaction.current.hasMoved = false;
    interaction.current.startX = e.clientX;
    interaction.current.startY = e.clientY;
  };

  const handleMouseMove = (e) => {
    if (!interaction.current.isDragging) return;
    const dx = e.clientX - interaction.current.startX;
    const dy = e.clientY - interaction.current.startY;
    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) interaction.current.hasMoved = true;
    transform.current.x += dx;
    transform.current.y += dy;
    interaction.current.startX = e.clientX;
    interaction.current.startY = e.clientY;
  };

  const handleMouseUp = (e) => {
    interaction.current.isDragging = false;
    if (!interaction.current.hasMoved) handleGraphClick(e);
  };

  const handleGraphClick = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const rawX = e.clientX - rect.left;
    const rawY = e.clientY - rect.top;
    const { x, y, k } = transform.current;
    const graphX = (rawX - x) / k;
    const graphY = (rawY - y) / k;
    const clicked = simulationNodes.find(n => {
      const dx = n.x - graphX;
      const dy = n.y - graphY;
      return Math.sqrt(dx*dx + dy*dy) < 30;
    });
    onNodeClick(clicked || null);
  };

  return (
    <canvas 
        ref={canvasRef} 
        width={width} 
        height={height} 
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={() => interaction.current.isDragging = false}
        className="cursor-move active:cursor-grabbing block" 
    />
  );
};

// --- 4. MAIN APP COMPONENT ---

export default function LonghornNetworkApp() {
  const [activeCase, setActiveCase] = useState(1);
  const [students, setStudents] = useState([]);
  const [links, setLinks] = useState([]);
  const [roommatePairs, setRoommatePairs] = useState({});
  const [referralPath, setReferralPath] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [referralTarget, setReferralTarget] = useState("DummyCompany");
  
  // Dynamic Sizing State
  const [dimensions, setDimensions] = useState({ w: 800, h: 600 });
  const containerRef = useRef(null);
  
  const [filterQuery, setFilterQuery] = useState("");

  // Resize Observer Logic
  useEffect(() => {
    const observer = new ResizeObserver((entries) => {
        if (entries[0]) {
            const { width, height } = entries[0].contentRect;
            setDimensions({ w: width, h: height });
        }
    });
    if (containerRef.current) observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  const fetchData = async (caseNum) => {
    try {
        setRoommatePairs({});
        setReferralPath([]);
        setSelectedStudent(null);

      const response = await fetch(`http://localhost:8080/api/load?case=${caseNum}`);
      const data = await response.json();
      updateStudentState(data);
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  const updateStudentState = (data) => {
      setStudents(data);
      const newLinks = [];
      for (let i = 0; i < data.length; i++) {
        for (let j = i + 1; j < data.length; j++) {
           let s1 = data[i]; let s2 = data[j];
           let strength = 0;
           
           if (s1.major === s2.major) strength += 2;
           if (s1.age === s2.age) strength += 1;
           const s1Interns = new Set(s1.internships.filter(x => x !== "None"));
           const hasShared = s2.internships.some(x => s1Interns.has(x) && x !== "None");
           if (hasShared) strength += 3;
           if (s1.roommateName === s2.name || s2.roommateName === s1.name) {
               strength += 4;
           }

           if (strength > 0) {
               newLinks.push({ 
                   source: s1.name, 
                   target: s2.name,
                   weight: strength
               });
           }
        }
      }
      setLinks(newLinks);
      if (selectedStudent) {
          const updatedSelected = data.find(s => s.name === selectedStudent.name);
          if (updatedSelected) setSelectedStudent(updatedSelected);
      }
  };

  useEffect(() => {
    fetchData(activeCase);
  }, [activeCase]);

  // -- UPDATED FILTERING LOGIC (SEARCH ALL FIELDS) --
  const filteredStudents = useMemo(() => {
    if (!filterQuery) return students;
    const query = filterQuery.toLowerCase();
    
    return students.filter(s => {
        // Check Name
        if (s.name.toLowerCase().includes(query)) return true;
        // Check Major
        if (s.major.toLowerCase().includes(query)) return true;
        // Check Internships
        if (s.internships && s.internships.some(i => i.toLowerCase().includes(query))) return true;
        // Check Age/GPA
        if (s.age.toString().includes(query)) return true;
        if (s.gpa.toString().includes(query)) return true;
        // Check Roommate Name
        if (s.roommateName && s.roommateName.toLowerCase().includes(query)) return true;
        
        return false;
    });
  }, [students, filterQuery]);

  const filteredLinks = useMemo(() => {
    const studentNames = new Set(filteredStudents.map(s => s.name));
    return links.filter(l => studentNames.has(l.source) && studentNames.has(l.target));
  }, [links, filteredStudents]);


  // 2. RUN ALGORITHMS
  const runGaleShapley = async () => {
    const response = await fetch(`http://localhost:8080/api/match`);
    const data = await response.json();
    const pairs = {};
    data.forEach(s => {
        if (s.roommateName && s.roommateName !== "null") pairs[s.name] = s.roommateName;
    });
    setRoommatePairs(pairs);
    updateStudentState(data);
  };

  const runDijkstra = async () => {
    if (!selectedStudent) return alert("Select a start node first!");
    const response = await fetch(`http://localhost:8080/api/referral?start=${selectedStudent.name}&company=${referralTarget}`);
    const pathArray = await response.json();
    setReferralPath(pathArray);
    if (pathArray.length === 0) alert("No path found.");
  };

  const runFriendRequests = async () => {
      try {
          const response = await fetch(`http://localhost:8080/api/friends`);
          const data = await response.json();
          updateStudentState(data);
      } catch (error) { console.error(error); }
  };

  const runChats = async () => {
      try {
          const response = await fetch(`http://localhost:8080/api/chat`);
          const data = await response.json();
          updateStudentState(data);
      } catch (error) { console.error(error); }
  };

  return (
    <div className="flex flex-col h-screen bg-slate-950 text-slate-100 font-sans overflow-hidden">
      
      <header className="z-50 bg-slate-900 border-b border-slate-800 p-4 flex justify-between items-center shadow-md shrink-0">
        <div className="flex items-center gap-3">
          <div className="bg-ut-orange p-2 rounded-lg shadow-lg shadow-orange-900/50">
            <img src={logo} alt="Longhorn Logo" className="w-8 h-8 object-contain" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">Longhorn Network Lab</h1>
            <p className="text-xs text-slate-400">UT ECE 422C • Fall 2025</p>
          </div>
        </div>
        <div className="flex bg-slate-800 p-1 rounded-lg border border-slate-700">
          {[1, 2, 3].map(num => (
            <button key={num} onClick={() => setActiveCase(num)} className={`px-4 py-1.5 text-sm rounded-md font-medium transition-all ${activeCase === num ? "bg-ut-orange text-white" : "text-slate-400 hover:text-white"}`}>
              Test Case {num}
            </button>
          ))}
        </div>
      </header>

      <div className="flex flex-1 relative min-h-0">
        <div className="absolute top-6 left-6 z-20 w-80 flex flex-col gap-4 pointer-events-none">
          
          <div className="pointer-events-auto bg-slate-900/90 backdrop-blur-md p-5 rounded-xl border border-slate-700 shadow-2xl">
            <h2 className="text-xs font-bold text-slate-400 mb-4 uppercase tracking-widest flex items-center gap-2">
               <div className="w-1 h-4 bg-ut-orange rounded-full"></div> Controls
            </h2>

            {/* FILTER SECTION */}
            <div className="mb-4">
                <div className="relative">
                    <input 
                        type="text" 
                        value={filterQuery}
                        onChange={(e) => setFilterQuery(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-700 rounded-lg pl-3 pr-10 py-2.5 text-sm text-white focus:border-ut-orange outline-none placeholder-slate-600"
                        placeholder="Filter by Name, Major, Job..."
                    />
                    <Filter size={16} className="absolute right-3 top-3 text-slate-500" />
                </div>
            </div>

            <button onClick={runGaleShapley} className="w-full flex items-center gap-3 bg-slate-800 hover:bg-emerald-600/20 border border-slate-700 text-slate-200 py-3 px-4 rounded-lg mb-2 transition-all">
                <Heart size={18} className="text-emerald-400"/> <span className="text-sm font-medium">Assign Roommates</span>
            </button>

            {/* FRIEND REQUEST BUTTON */}
            <button onClick={runFriendRequests} className="w-full flex items-center gap-3 bg-slate-800 hover:bg-indigo-600/20 border border-slate-700 text-slate-200 py-3 px-4 rounded-lg mb-2 transition-all">
                <UserPlus size={18} className="text-indigo-400"/> <span className="text-sm font-medium">Friend Requests</span>
            </button>

            <button onClick={runChats} className="w-full flex items-center gap-3 bg-slate-800 hover:bg-purple-600/20 border border-slate-700 text-slate-200 py-3 px-4 rounded-lg mb-4 transition-all">
                <MessageSquare size={18} className="text-purple-400"/> <span className="text-sm font-medium">Run Chat Threads</span>
            </button>

            <div className="space-y-3">
              <div className="relative">
                <input type="text" value={referralTarget} onChange={(e) => setReferralTarget(e.target.value)} className="w-full bg-slate-950 border border-slate-700 rounded-lg pl-10 pr-3 py-2.5 text-sm text-white focus:border-ut-orange outline-none" placeholder="Target Company..." />
                <Briefcase size={16} className="absolute left-3 top-3 text-slate-500" />
              </div>
              <button onClick={runDijkstra} className={`w-full flex items-center gap-3 border py-3 px-4 rounded-lg transition-all ${selectedStudent ? "bg-slate-800 border-slate-700 hover:bg-blue-600/20 text-slate-200" : "bg-slate-900 border-slate-800 text-slate-600 cursor-not-allowed"}`} disabled={!selectedStudent}>
                <Search size={18} className={selectedStudent ? "text-blue-400" : "text-slate-600"}/> <span className="text-sm font-medium">Find Referral Path</span>
              </button>
            </div>
          </div>
        </div>

        {/* CONTAINER WITH REF FOR RESIZE OBSERVER */}
        <div ref={containerRef} className="flex-1 bg-slate-950 overflow-hidden">
           <SimpleGraph 
               nodes={filteredStudents} 
               links={filteredLinks} 
               width={dimensions.w} 
               height={dimensions.h} 
               onNodeClick={setSelectedStudent} 
               selectedNode={selectedStudent} 
               roommatePairs={roommatePairs} 
               referralPath={referralPath} 
            />
        </div>

        {selectedStudent && (
        <div className="w-96 bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col z-30 animate-in slide-in-from-right duration-300 h-full">
            <div className="p-6 border-b border-slate-800 bg-slate-900 relative overflow-hidden shrink-0">
                <div className="relative z-10 flex items-center gap-4">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-ut-orange to-orange-800 flex items-center justify-center text-3xl font-bold text-white shadow-lg">
                        {selectedStudent.name.charAt(0)}
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-white">{selectedStudent.name}</h2>
                        <span className="inline-block px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700 mt-1">{selectedStudent.major}</span>
                    </div>
                </div>
            </div>
            
            <div className="flex-1 overflow-y-auto p-6 space-y-8">
                <section>
                  <div className="flex items-center justify-between mb-3">
                      <h3 className="text-xs uppercase text-slate-500 font-bold flex items-center gap-2">
                        <MessageSquare size={14} /> Chat History
                      </h3>
                  </div>
                  
                  <div className="bg-slate-950 rounded-xl border border-slate-800 overflow-hidden">
                    <div className="p-3 border-b border-slate-800 bg-slate-900/50">
                         <p className="text-xs text-slate-400">Friends (Click 'Friend Requests')</p>
                         <div className="flex flex-wrap gap-2 mt-2">
                            {selectedStudent.friends && selectedStudent.friends.length > 0 ? (
                                selectedStudent.friends.map(f => (
                                    <span key={f} className="flex items-center gap-1.5 text-xs text-slate-300 bg-slate-800 px-2 py-1 rounded-full border border-slate-700">
                                        <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span> {f}
                                    </span>
                                ))
                            ) : <span className="text-slate-600 text-xs italic">No friends added</span>}
                         </div>
                    </div>
                    <div className="h-48 overflow-y-auto p-4 space-y-3">
                      {selectedStudent.chatHistory && Object.keys(selectedStudent.chatHistory).length > 0 ? (
                        Object.entries(selectedStudent.chatHistory).map(([partnerName, msgs]) => (
                            <div key={partnerName} className="space-y-2">
                                <div className="text-[10px] text-center text-slate-600 uppercase tracking-wider">Chat with {partnerName}</div>
                                {msgs.map((msg, i) => {
                                    const isMe = msg.startsWith(selectedStudent.name);
                                    const text = msg.includes(": ") ? msg.split(": ")[1] : msg;
                                    return (
                                        <div key={i} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
                                            <div className={`max-w-[85%] p-2 rounded-lg text-xs ${
                                                isMe 
                                                ? "bg-ut-orange text-white rounded-br-none" 
                                                : "bg-slate-800 text-slate-300 rounded-bl-none"
                                            }`}>
                                                {text}
                                            </div>
                                        </div>
                                    )
                                })}
                            </div>
                        ))
                      ) : (
                        <div className="h-full flex flex-col items-center justify-center text-slate-600">
                            <MessageSquare size={24} className="mb-2 opacity-20" />
                            <p className="text-xs italic">No messages yet.</p>
                        </div>
                      )}
                    </div>
                  </div>
                </section>

                <section>
                  <h3 className="text-xs uppercase text-slate-500 font-bold mb-2">Roommate</h3>
                  <p className="text-emerald-400 font-medium">{selectedStudent.roommateName && selectedStudent.roommateName !== "null" ? "Matched with " + selectedStudent.roommateName : "Unmatched"}</p>
                </section>
                <section>
                   <h3 className="text-xs uppercase text-slate-500 font-bold mb-2">Internships</h3>
                   <div className="flex flex-wrap gap-2">
                       {selectedStudent.internships && selectedStudent.internships.map(i => <span key={i} className="px-2 py-1 bg-blue-900/30 text-blue-300 text-xs rounded border border-blue-800">{i}</span>)}
                   </div>
                </section>
            </div>
            
            <div className="p-4 border-t border-slate-800 text-center shrink-0">
                <button onClick={() => setSelectedStudent(null)} className="text-xs text-slate-500 hover:text-white">Close Details</button>
            </div>
        </div>
        )}
      </div>
    </div>
  );
}