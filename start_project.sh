#!/bin/bash

# Função para encerrar os processos ao pressionar Ctrl+C
cleanup() {
    echo -e "\n\nEncerrando serviços..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    exit
}

trap cleanup SIGINT

echo "--- Iniciando Sistema de ROI Lost Ark ---"

# Iniciar Backend
echo "Iniciando Backend (Java Spring Boot)..."
cd backend
./mvnw spring-boot:run > /dev/null 2>&1 &
BACKEND_PID=$!

# Iniciar Frontend
echo "Iniciando Frontend (Angular)..."
cd ../frontend
npx ng serve --no-open > /dev/null 2>&1 &
FRONTEND_PID=$!

echo "Aguardando inicialização (aprox. 5 segundos)..."
sleep 5   

# Abrir URLs automaticamente
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:4200" # Abre o Frontend
    xdg-open "http://localhost:8080" # Abre o Backend (agora com resposta amigável)
elif command -v google-chrome > /dev/null; then
    google-chrome "http://localhost:4200" "http://localhost:8080"
fi

echo "------------------------------------------"
echo "Serviços em execução:"
echo "Frontend: http://localhost:4200"
echo "Backend:  http://localhost:8080"
echo "------------------------------------------"
echo "Pressione Ctrl+C para parar todos os serviços."

# Aguarda os processos
wait
