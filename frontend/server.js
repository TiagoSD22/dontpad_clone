const server = Bun.serve({
  port: 3000,
  async fetch(req) {
    const url = new URL(req.url);
    
    // Serve index.html for root
    if (url.pathname === '/') {
      return new Response(Bun.file('./public/index.html'));
    }
    
    // Serve static files
    if (url.pathname === '/client.js') {
      return new Response(Bun.file('./public/client.js'), {
        headers: { 'Content-Type': 'application/javascript' }
      });
    }
    
    return new Response('Not Found', { status: 404 });
  },
});

console.log(`🚀 Frontend server running at http://localhost:${server.port}`);
console.log(`📝 Open http://localhost:${server.port} in your browser`);
