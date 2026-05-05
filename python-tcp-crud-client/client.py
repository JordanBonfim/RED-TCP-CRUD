import sys
import os

# Adiciona a pasta protoc para encontrar catalog_pb2 e catalog_pb2_grpc
sys.path.append(os.path.join(os.path.dirname(__file__), 'protoc'))

import grpc
import catalog_pb2 
import catalog_pb2_grpc  

def run():
    print("Sistemas online. Iniciando interface de comando Python...")
    
    # Endereço do servidor 
    server_address = 'localhost:5090'

    try:
        
        with grpc.insecure_channel(server_address) as channel:
            stub = catalog_pb2_grpc.MovieServiceStub(channel)
            print(f"Link estabelecido com o servidor em {server_address}.\n")

            while True:
                print("="*50)
                print("   PAINEL DE CONTROLE DE FILMES   ")
                print("="*50)
                print("1. Listar filmes por Ator")
                print("2. Listar filmes por Categoria")
                print("3. Buscar filme específico (Read - por ID)")
                print("0. Desconectar e encerrar sistemas")
                print("="*50)

                opcao = input("Escolha uma opção: ")

                if opcao == "1":
                    ator = input("Informe o nome do ator (ex: Winsor McCay): ")
                    request = catalog_pb2.ListByActorRequest(actor_name=ator)
                    response = stub.ListMoviesByActor(request)

                    if not response.movies:
                        print("Nenhum registro localizado para este ator no banco de dados.")
                    for movie in response.movies:
                        print(f"[{movie.year}] {movie.title} - Elenco: {', '.join(movie.cast)}")

                elif opcao == "2":
                    categoria = input("Informe a categoria (ex: Animation, Comedy): ")
                    request = catalog_pb2.ListByCategoryRequest(category=categoria)
                    response = stub.ListMoviesByCategory(request)

                    if not response.movies:
                        print("Nenhum registro localizado para esta categoria no banco de dados.")
                    for movie in response.movies:
                        print(f"[{movie.year}] {movie.title} - Gêneros: {', '.join(movie.genres)}")

                elif opcao == "3":
                    filme_id = input("Informe o ID do filme (formato ObjectId do MongoDB Atlas): ")
                    request = catalog_pb2.ReadMovieRequest(id=filme_id)
                    response = stub.ReadMovie(request)

                    # Verifica se o ID foi retornado (sinal de que o filme existe)
                    if response.id:
                        print("\n--- DADOS DO FILME ---")
                        print(f"Título: {response.title}")
                        print(f"Ano: {response.year}")
                        print(f"Resumo: {response.plot}")
                        print(f"Diretores: {', '.join(response.directors)}")
                    else:
                        print("Filme não localizado nos registros do servidor, senhor.")

                elif opcao == "0":
                    print("Encerrando conexão TCP")
                    break
                else:
                    print("Comando não reconhecido. Por favor, tente novamente.")

    except grpc.RpcError as e:
        print(f"Falha na comunicação com o servidor. Detalhes do erro:\n{e}")
    except KeyboardInterrupt:
        print("\nSinal de interrupção manual recebido. Encerrando.")

if __name__ == "__main__":
    run()