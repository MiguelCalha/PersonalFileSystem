# 1. Sistema de Arquivos PFS (Personal File System) em JavaFX:

Bem-vindo ao Sistema de Arquivos PFS, uma aplicação JavaFX projetada para simular a eficiente gestão de arquivos e diretórios. Este sistema utiliza o conceito de um Tipo Abstrato de Dados (ADT) Tree para representar uma hierarquia organizada de arquivos. Cada nó na árvore representa um arquivo ou diretório e armazena informações essenciais, como nome e tipo etc que incorpora padrões de software para garantir uma implementação sólida e eficiente.

# 2. Detalhes sobre a estrutura de dados desenhada para suportar o PFS:

A estrutura de dados para suportar o PFS baseia-se no conceito de um Tipo Abstrato de Dados (ADT) Tree. O núcleo desta estrutura representa um ou vários nós na árvore de ficheiros e diretórios. Cada nó contém informações como nome, tipo (ficheiro ou diretório), e uma lista de nós filhos.

# 3. Previsão de como irá ser persistida a árvore do PFS:

A árvore do PFS será guardada através de serialização. Ao fechar a aplicação, a estrutura da árvore será serializada e gravada num ficheiro. Ao iniciar, a aplicação teremos que abrir o ficheiro criado anteriormente, reconstruindo assim a árvore de forma a mostrar todas as pasta e respetivos ficheiros.

# 4. Descrição das duas funcionalidades extra:

### Pesquisar por nome de diretoria : 
* Permite que o utilizador procure diretórios específicos dentro do sistema de ficheiros PFS com base no nome. Isto proporciona uma forma eficiente de encontrar diretórios específicos, especialmente quando a árvore de ficheiros é extensa.
  
### Exportar ficheiro atraves de ZIP :
* Permite ao utilizador criar um arquivo ZIP contendo um ou mais ficheiros selecionados ou um diretório inteiro. Isto é útil para compactar e organizar ficheiros antes de partilhar ou realizar backups.

# 6. Resumo do trabalho/código desenvolvido:

O código desenvolvido consiste numa aplicação JavaFX que simula a gestão de um sistema de ficheiros PFS. A estrutura da árvore é gerida por uma ADT Tree, onde cada nó representa um ficheiro ou diretório. São permitidas as operações básicas, como criação, edição, cópia, exclusão e navegação. A preservação da árvore é realizada por meio de codificação para garantir a persistência das informações entre execuções.

Nesta primeira fase fora desenvolvidos os seguintes métodos que permitiram posteriormente as funcionalidades descritas no 3.3.1:
* Criar - cria um elemento vazio;
```java
public void createFile(Position<String> parent, String fileName, boolean isLocked, String fileContent)
```
* Copiar - Cria um novo elemento com todo o conteúdo do elemento de origem;
 ```java
public void copyElement(Position<String> source, Position<String> destination) {
```
* Mover - Move um elemento de uma diretoria para a outra;
```java
public void moveElement(Position<String> sourcePosition, Position<String> destinationPosition)
```
* Renomear - Altera o nome do elemento;
```java
public void renameElement(Position<String> elementPosition, String newName)
```
* Remover - Remover um elemento;
 ```java
public void deleteElement(Position<String> elementPosition) 
```
* Visualizar - Visualizar o conteúdo de um ficheiro;
```java
 public void visualizeFile(Position<String> filePosition)
```
* Editar - Editar o conteúdo de um ficheiro, caso esse tenha como status unlocked.
```java
 public void editFile(Position<String> position, String newContent)
```

* Além das operações atómicas, também foram implementadas funções que permitem visualizar os dados guardados da TreeLinked em TreeView. O grupo decidiu que era importante estudar como deverá ser feita a persistência de dados no futuro,
* escolhendo (por agora) guardar os dados de forma "fisica" num path do computador. Quando abrimos uma pasta, esta e os seus conteúdos são automaticamente guardados na TreeLinked e depois convertidos à TreeView do JavaFX.
  


# 7. Mockup Idealizado para a aplicação
(![image](https://github.com/estsetubal-pa-2023-24/projeto-en-pm_201902037/blob/main/MOCKUP1.png?raw=true) 

